package app.tostrr.domain.sync.usecase

import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.sync.SyncScheduler
import app.tostrr.domain.sync.SyncSettingsRepository
import org.koin.core.annotation.Factory

/**
 * Makes the background sync match the settings: scheduled at the chosen interval while logged in with auto sync on,
 * cancelled otherwise. Called at startup, after login and when the settings change.
 */
@Factory
class StartBackgroundSyncUseCase(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
    private val settings: SyncSettingsRepository,
) {
    operator fun invoke() {
        val current = settings.settings.value
        if (auth.isLoggedIn() && current.autoSync) scheduler.schedule(current.interval) else scheduler.cancel()
    }
}
