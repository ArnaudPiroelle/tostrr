package app.tostrr.data.notification

import android.content.Context
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.notification.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/** Keeps the turned-off kinds in shared preferences, next to the other settings; all on is stored as nothing at all. */
@Single
class NotificationSettingsRepositoryImpl(context: Context) : NotificationSettingsRepository {
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        NotificationSettings(
            disabled = prefs.getStringSet(KEY_DISABLED, emptySet()).orEmpty()
                .mapNotNull { name -> NotificationKind.entries.firstOrNull { it.name == name } }
                .toSet(),
        ),
    )
    override val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    override fun save(settings: NotificationSettings) {
        _settings.value = settings
        prefs.edit().apply {
            if (settings.disabled.isEmpty()) remove(KEY_DISABLED)
            else putStringSet(KEY_DISABLED, settings.disabled.map { it.name }.toSet())
        }.apply()
    }

    private companion object {
        const val KEY_DISABLED = "notifications_disabled"
    }
}
