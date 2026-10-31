package app.tostrr.domain.sync

import kotlinx.coroutines.flow.StateFlow

/** The user's background sync preferences. */
interface SyncSettingsRepository {
    val settings: StateFlow<SyncSettings>

    fun save(settings: SyncSettings)
}
