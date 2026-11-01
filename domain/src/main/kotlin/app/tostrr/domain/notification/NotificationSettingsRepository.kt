package app.tostrr.domain.notification

import kotlinx.coroutines.flow.StateFlow

/** The user's choice of notifications. */
interface NotificationSettingsRepository {
    val settings: StateFlow<NotificationSettings>

    fun save(settings: NotificationSettings)
}
