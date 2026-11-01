package app.tostrr.domain.notification.usecase

import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettingsRepository
import org.koin.core.annotation.Factory

/** Turns one kind of notification on or off; off, the next syncs no longer post it. */
@Factory
class SetNotificationEnabledUseCase(private val repository: NotificationSettingsRepository) {
    operator fun invoke(kind: NotificationKind, enabled: Boolean) {
        val current = repository.settings.value
        if (current.isEnabled(kind) == enabled) return
        repository.save(current.with(kind, enabled))
    }
}
