package app.tostrr.domain.sync.usecase

import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettingsRepository
import org.koin.core.annotation.Factory

/** Saves the time between two background syncs and moves the running schedule to it. */
@Factory
class SetSyncIntervalUseCase(
    private val repository: SyncSettingsRepository,
    private val startBackgroundSyncUseCase: StartBackgroundSyncUseCase,
) {
    operator fun invoke(interval: SyncInterval) {
        val current = repository.settings.value
        if (interval == current.interval) return
        repository.save(current.copy(interval = interval))
        startBackgroundSyncUseCase()
    }
}
