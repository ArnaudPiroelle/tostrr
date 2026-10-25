# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Tostrr is an Android app (package `app.tostrr`) that tracks Tesla vehicle orders: it logs into the Tesla owner API, polls the (undocumented) order endpoints, shows order progress, records a change history, sends notifications, and offers home-screen widgets and a native delivery-day checklist. Kotlin, Jetpack Compose (Material 3), Glance widgets, WorkManager, Room, OkHttp, kotlinx.serialization. DI is Koin with Koin Annotations (KSP): classes are annotated `@Single` / `@Factory` (use cases) / `@KoinViewModel` / `@KoinWorker` and picked up by `@ComponentScan`: one Koin module per Gradle module, `DomainModule` (`app.tostrr.domain.di`) and `DataModule` (`app.tostrr.data.di`) included by `AppModule` in `:app`. Our own classes always carry the annotation themselves (take `Context` and derive paths inside rather than being built in a provider); the module classes only provide types we don't construct (`DataModule`: OkHttp, Room database/DAO, `WorkManager`; `AppModule`: the app `CoroutineScope`); `TostrrApp.onCreate()` calls `startKoin`. `KOIN_CONFIG_CHECK` (in `:data` and `:app`) makes a missing dependency fail the build; it runs per Gradle module and can't see across them, so it is off in `:domain` and the whole graph is checked by `KoinGraphTest` in `:app` (koin-test `verify()`), which fails if a domain interface has no implementation. Koin is pinned to 4.1.x because 4.2 needs Kotlin 2.3. WorkManager is initialised through Koin's `workManagerFactory()` (default initializer removed in the manifest); Glance widgets get dependencies as `KoinComponent`s.

User-facing strings are hardcoded in French in the Kotlin code (not in `strings.xml`); keep new UI text in French. Code comments and identifiers are in English.

## Commands

The system `java` here is a JRE without javac and Android Studio's JBR is too new for AGP 8.11, so set `JAVA_HOME` explicitly (shell is fish; wrap multi-line scripts in `bash -c`):

```bash
JAVA_HOME=/home/arnaud/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew :app:assembleDebug
```

- Unit tests: `./gradlew :domain:test :app:testDebugUnitTest` (the latter is only the Koin graph check)
- Single test: `./gradlew :domain:test --tests 'app.tostrr.domain.alert.OrderAlertsTest'` (append `.methodName` for one method)
- Install: `ANDROID_SERIAL=<serial> ./gradlew :app:installDebug` — always set `ANDROID_SERIAL`, otherwise it installs on every connected device.

There is no lint/format config beyond `kotlin.code.style=official`. Tests are plain JUnit 4 under `domain/src/test`, covering only the pure `:domain` module, plus `KoinGraphTest` under `app/src/test` (no Android/instrumented tests).

`api/` contains only stale build output from removed modules; it is not part of the Gradle build.

## Architecture

**Modules / layers**: `:domain` (Kotlin JVM library, no Android: JSON → model, rules; this is what's unit-tested) ← `:data` (Android library: network, auth, persistence, notifications, background work; exposes `:domain` as `api`) ← `:app` (`ui/` `MainActivity`, Compose screens + ViewModels, `widget/` Glance: one package per widget, `widget.order` / `widget.countdown`, with its composables in a `components` sub-package; the receivers, the shared config activity and `OrderRefKey` stay in `app.tostrr.widget`, because launchers bind placed widgets to the receiver's class name and moving it breaks them, `TostrrApp`). Clean-architecture rules: repositories are interfaces in `:domain` (`OrderRepository`, `ChecklistRepository`, `AuthRepository`, `OptionRepository`, `DeliveryCenterRepository`, `SyncScheduler`), implemented in `:data` (`OrderRepositoryImpl`, `ChecklistRepositoryImpl`, `TeslaAuth`, `OptionCatalog`, `DeliveryCenterCatalog`, `RefreshScheduler`; Koin binds each to its interface). ViewModels, widgets and `TostrrApp` never touch repositories: they go through use cases in `app.tostrr.domain.<feature>.usecase` (`@Factory`, one action each, `operator fun invoke`), injected under names ending in `UseCase`. Exceptions shown to the user (`AuthException`, `SessionExpiredException`, `ApiException`) live in `app.tostrr.domain.error`. Each module is split into sub-packages by concern: `app.tostrr.domain.{order, history, alert, checklist, deliverycenter, auth, sync, error, layout}` (`order` holds `Order`, `OrderMapper`, the step detectors and the delivery countdown; `history` holds `JsonDiff` and `HistoryIgnore`) and `app.tostrr.data.{api, auth, order, notification, sync, checklist, catalog, layout, di}`. Put new classes in the matching sub-package (tests mirror it). `:data` must not reference `:app`: notifications open the launcher intent and use the application icon rather than `MainActivity` / `R`.

**Sync pipeline** — the core flow, centred on `OrderRepositoryImpl.refresh()` (reached through `RefreshOrdersUseCase`):
1. `TeslaAuth` provides a valid access token (OAuth2 code + PKCE with Tesla's public `ownerapi` client; redirect `tesla://auth/callback` is caught by the manifest intent filter on `MainActivity`, or pasted manually). Tokens are encrypted with a Keystore key in `TokenStore`.
2. `TeslaApi` fetches `GET /api/1/users/orders` and, per order, the `/tasks` endpoint. Payloads are kept schema-less as `JsonObject`.
3. `OrderMapper.map(order, details, fetchedAt)` builds the `Order` model, including the fixed list of `OrderStep`s. Every field access is defensive and many dates are found by key name anywhere in the tree (`deepFind`) because Tesla moves fields around.
4. The raw JSON of old vs new is pruned by `HistoryIgnore` and diffed by `JsonDiff`; deltas become `Change` entries in the per-order history.
5. `OrderNotifier` (using rules in `OrderAlerts`) posts step-change, option-change, delivery-window, reminder, digest and sync-problem notifications.
6. State is persisted as JSON files in `filesDir` (`orders.json`, `history.json`) and exposed as `StateFlow`s; every pull is also archived as gzipped JSON in `getExternalFilesDir("snapshots")/<order>/` (pullable over adb for analysing real payloads).
7. Repositories expose state as flows, never listeners: `WidgetRefresher` (started in `TostrrApp`) collects the orders flow and runs `OrderWidget`/`CountdownWidget` `updateAll` on each new value (every sync emits one, as `fetchedAtEpochMs` changes).

Refreshes run from the UI (`MainViewModel`) and from `RefreshWorker` (15-minute periodic WorkManager job, scheduled by `RefreshScheduler` when logged in). A `Mutex` serialises them so diffs never see half-updated state. `AuthException`/`SessionExpiredException` mean "drop the session"; other failures retry.

**Unverified rules**: Tesla doesn't document how steps like "Production terminée", "Documents envoyés" or "Véhicule immatriculé" appear. `StepDetector` / `PayloadStepDetectors`, `HistoryIgnore` and parts of `OrderMapper` are guesses, not observed changes in real payloads. Treat them as hypotheses: when real snapshots with new values are available, check them against these rules before trusting either. Swap step logic through `StepDetectors` rather than editing `OrderMapper`.

**Catalogues**: `OptionCatalog` downloads option-code labels from tesla-order-status-tracker.de (best effort, cached, never fails a sync). `DeliveryCenterCatalog` reads the bundled `data/src/main/assets/delivery_centers.json`, a snapshot of tesla.com/findus; regenerate it with the project skill `refresh-delivery-centers` (`.claude/skills/`).

**Arranging the order detail** (ported from the sibling teslarr app): `domain/layout` has the blocks (`DetailBlock`, declaration order = default), the saved `BlockLayout(order, hidden)` (empty = default, never stored) and the pure `BlockLayouts` functions (resolve with unknown ids dropped and new blocks slotted before their next default neighbour, move, toggle), used through `DetailLayoutRepository` and its use cases. `DetailLayoutRepositoryImpl` keeps it as JSON in the `tostrr_layouts` shared preferences. The detail top bar opens `DetailLayoutSheet` (arrows and hide/show, applied at once, plus reset); the hero header stays fixed. A new block only needs an enum entry plus its title, blurb and icon in the sheet and its branch in `DetailScreen`.

**Checklist**: delivery-day checklist content is defined in `domain/checklist/DeliveryChecklist.kt`; answers and photos are stored per order in Room (`ChecklistDatabase`, version 1, no migrations yet — bumping the schema needs a migration) via `ChecklistRepository`, with photos under `filesDir/checklist_photos` shared through the `FileProvider`.

**Navigation**: Navigation 3 (pinned to 1.1.x: 1.2 needs AGP 9.1). A single `MainActivity` owns the back stack (`rememberNavBackStack`) of `@Serializable` `Screen` keys (`ui/navigation/Screen.kt`: `Login`, `Orders`, `OrderDetail(reference)`, `Checklist(reference)`) shown by a `NavDisplay`; each entry gets its own saved state and `ViewModelStore` through the entry decorators. Screen arguments come from the key, not the `SavedStateHandle`: `ChecklistViewModel` takes the reference as a Koin `@InjectedParam` (`koinViewModel { parametersOf(reference) }`). Logging in or out replaces the whole stack. Widgets and launcher shortcuts use the internal deep links `tostrr://order/<reference>` (`MainActivity.orderUri`) and `tostrr://orders`. Shortcuts: a static "Commandes" one (`res/xml/shortcuts.xml`, whose `targetPackage` must match the applicationId) and up to three dynamic ones for the latest orders awaiting delivery, kept in sync with the orders flow by `shortcut/OrderShortcuts` (started in `TostrrApp`).

## Conventions

- Use only even `dp` values in Compose code.
- Don't commit unless explicitly asked.
- Don't `force-stop` the app on a device that has widgets/notifications set up: it resets widgets and cancels notifications.
