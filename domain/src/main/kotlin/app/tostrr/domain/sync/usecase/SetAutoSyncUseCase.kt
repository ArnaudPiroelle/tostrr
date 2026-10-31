package app.tostrr.domain.sync.usecase

import app.tostrr.domain.sync.SyncSettingsRepository
import org.koin.core.annotation.Factory

/** Turns the background sync on or off; off, the orders are only fetched on demand. */
@Factory
class SetAutoSyncUseCase(
    private val repository: SyncSettingsRepository,
    private val startBackgroundSyncUseCase: StartBackgroundSyncUseCase,
) {
    operator fun invoke(enabled: Boolean) {
        val current = repository.settings.value
        if (enabled == current.autoSync) return
        repository.save(current.copy(autoSync = enabled))
        startBackgroundSyncUseCase()
    }
}
