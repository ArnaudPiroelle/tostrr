package app.tostrr.data.sync

import android.content.Context
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.SyncSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/** Keeps the sync settings in shared preferences; a default value is stored as nothing at all. */
@Single
class SyncSettingsRepositoryImpl(context: Context) : SyncSettingsRepository {
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)
    private val defaults = SyncSettings()

    private val _settings = MutableStateFlow(
        SyncSettings(
            autoSync = prefs.getBoolean(KEY_AUTO_SYNC, defaults.autoSync),
            interval = SyncInterval.fromMinutes(prefs.getLong(KEY_INTERVAL, defaults.interval.minutes)),
        ),
    )
    override val settings: StateFlow<SyncSettings> = _settings.asStateFlow()

    override fun save(settings: SyncSettings) {
        _settings.value = settings
        prefs.edit().apply {
            if (settings.autoSync == defaults.autoSync) remove(KEY_AUTO_SYNC) else putBoolean(KEY_AUTO_SYNC, settings.autoSync)
            if (settings.interval == defaults.interval) remove(KEY_INTERVAL) else putLong(KEY_INTERVAL, settings.interval.minutes)
        }.apply()
    }

    private companion object {
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_INTERVAL = "sync_interval_minutes"
    }
}
