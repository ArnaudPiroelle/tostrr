package app.tostrr.domain.notification.usecase

import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.notification.NotificationSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Which kinds of notifications are turned on. */
@Factory
class ObserveNotificationSettingsUseCase(private val repository: NotificationSettingsRepository) {
    operator fun invoke(): StateFlow<NotificationSettings> = repository.settings
}
