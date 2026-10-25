# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tostrr is an Android app (package `com.tostrr`) that tracks Tesla vehicle orders: it logs into the Tesla owner API, polls the (undocumented) order endpoints, shows order progress, records a change history, sends notifications, and offers home-screen widgets and a native delivery-day checklist. Kotlin, Jetpack Compose (Material 3), Glance widgets, WorkManager, Room, OkHttp, kotlinx.serialization. DI is Koin with Koin Annotations (KSP): classes are annotated `@Single` / `@KoinViewModel` / `@KoinWorker` and picked up by `@ComponentScan`: one Koin module per Gradle module, `DataModule` in `:data` (`com.tostrr.data.di`) included by `AppModule` in `:app`. Our own classes always carry the annotation themselves (take `Context` and derive paths inside rather than being built in a provider); the module classes only provide types we don't construct (`DataModule`: OkHttp, Room database/DAO, `WorkManager`; `AppModule`: the app `CoroutineScope`). A `:data` dependency implemented in `:app` (e.g. `OrderRefreshListener`) is marked `@Provided` so the `:data` check accepts it; `TostrrApp.onCreate()` calls `startKoin`. `KOIN_CONFIG_CHECK` makes a missing dependency fail the build. Koin is pinned to 4.1.x because 4.2 needs Kotlin 2.3. WorkManager is initialised through Koin's `workManagerFactory()` (default initializer removed in the manifest); Glance widgets get dependencies as `KoinComponent`s.

User-facing strings are hardcoded in French in the Kotlin code (not in `strings.xml`); keep new UI text in French. Code comments and identifiers are in English.

## Commands

The system `java` here is a JRE without javac and Android Studio's JBR is too new for AGP 8.11, so set `JAVA_HOME` explicitly (shell is fish; wrap multi-line scripts in `bash -c`):

```bash
JAVA_HOME=/home/arnaud/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew :app:assembleDebug
```

- Unit tests: `./gradlew :domain:test`
- Single test: `./gradlew :domain:test --tests 'com.tostrr.domain.OrderAlertsTest'` (append `.methodName` for one method)
- Install: `ANDROID_SERIAL=<serial> ./gradlew :app:installDebug` — always set `ANDROID_SERIAL`, otherwise it installs on every connected device.

There is no lint/format config beyond `kotlin.code.style=official`. Tests are plain JUnit 4 under `domain/src/test`, covering only the pure `:domain` module (no Android/instrumented tests).

`api/` contains only stale build output from removed modules; it is not part of the Gradle build.

## Architecture

**Modules / layers**: `:domain` (Kotlin JVM library, no Android: JSON → model, rules; this is what's unit-tested) ← `:data` (Android library: network, auth, persistence, notifications, background work; exposes `:domain` as `api`) ← `:app` (`ui/` Compose screens + ViewModels, `widget/` Glance, `MainActivity`, `TostrrApp`). Each module is split into sub-packages by concern: `com.tostrr.domain.{order, history, alert, checklist, deliverycenter}` (`order` holds `Order`, `OrderMapper`, the step detectors and the delivery countdown; `history` holds `JsonDiff` and `HistoryIgnore`) and `com.tostrr.data.{api, auth, order, notification, sync, checklist, catalog, di}`. Put new classes in the matching sub-package (tests mirror it). `:data` must not reference `:app`: notifications open the launcher intent and use the application icon rather than `MainActivity` / `R`.

**Sync pipeline** — the core flow, centred on `OrderRepository.refresh()`:
1. `TeslaAuth` provides a valid access token (OAuth2 code + PKCE with Tesla's public `ownerapi` client; redirect `tesla://auth/callback` is caught by the manifest intent filter on `MainActivity`, or pasted manually). Tokens are encrypted with a Keystore key in `TokenStore`.
2. `TeslaApi` fetches `GET /api/1/users/orders` and, per order, the `/tasks` endpoint. Payloads are kept schema-less as `JsonObject`.
3. `OrderMapper.map(order, details, fetchedAt)` builds the `Order` model, including the fixed list of `OrderStep`s. Every field access is defensive and many dates are found by key name anywhere in the tree (`deepFind`) because Tesla moves fields around.
4. The raw JSON of old vs new is pruned by `HistoryIgnore` and diffed by `JsonDiff`; deltas become `Change` entries in the per-order history.
5. `OrderNotifier` (using rules in `OrderAlerts`) posts step-change, option-change, delivery-window, reminder, digest and sync-problem notifications.
6. State is persisted as JSON files in `filesDir` (`orders.json`, `history.json`) and exposed as `StateFlow`s; every pull is also archived as gzipped JSON in `getExternalFilesDir("snapshots")/<order>/` (pullable over adb for analysing real payloads).
7. `OrderRefreshListener` (implemented in `:app` by `WidgetRefresher`) triggers `OrderWidget`/`CountdownWidget` `updateAll`.

Refreshes run from the UI (`MainViewModel`) and from `RefreshWorker` (15-minute periodic WorkManager job, scheduled by `RefreshScheduler` when logged in). A `Mutex` serialises them so diffs never see half-updated state. `AuthException`/`SessionExpiredException` mean "drop the session"; other failures retry.

**Unverified rules**: Tesla doesn't document how steps like "Production terminée", "Documents envoyés" or "Véhicule immatriculé" appear. `StepDetector` / `PayloadStepDetectors`, `HistoryIgnore` and parts of `OrderMapper` are guesses, not observed changes in real payloads. Treat them as hypotheses: when real snapshots with new values are available, check them against these rules before trusting either. Swap step logic through `StepDetectors` rather than editing `OrderMapper`.

**Catalogues**: `OptionCatalog` downloads option-code labels from tesla-order-status-tracker.de (best effort, cached, never fails a sync). `DeliveryCenterCatalog` reads the bundled `data/src/main/assets/delivery_centers.json`, a snapshot of tesla.com/findus; regenerate it with the project skill `refresh-delivery-centers` (`.claude/skills/`).

**Checklist**: delivery-day checklist content is defined in `domain/checklist/DeliveryChecklist.kt`; answers and photos are stored per order in Room (`ChecklistDatabase`, version 1, no migrations yet — bumping the schema needs a migration) via `ChecklistRepository`, with photos under `filesDir/checklist_photos` shared through the `FileProvider`.

**Navigation**: a single `MainActivity` with a string-route `NavHost` (`login`, `orders`, `order/{ref}`, `order/{ref}/checklist`). Widgets open an order through the internal deep link `tostrr://order/<reference>`.

## Conventions

- Use only even `dp` values in Compose code.
- Don't commit unless explicitly asked.
- Don't `force-stop` the app on a device that has widgets/notifications set up: it resets widgets and cancels notifications.
