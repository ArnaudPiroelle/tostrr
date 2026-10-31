package app.tostrr.domain.sync.usecase

import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.SyncSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Whether the orders are fetched in the background, and how often. */
@Factory
class ObserveSyncSettingsUseCase(private val repository: SyncSettingsRepository) {
    operator fun invoke(): StateFlow<SyncSettings> = repository.settings
}
