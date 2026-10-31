package app.tostrr.ui.settings.components

import androidx.annotation.StringRes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun NotificationsSection(notifications: NotificationSettings, onChange: (NotificationKind, Boolean) -> Unit) {
    SettingsSection(stringResource(R.string.settings_notifications_section)) {
        NotificationKind.entries.forEachIndexed { index, kind ->
            if (index > 0) HorizontalDivider(color = TostrrColors.Border)
            SwitchRow(
                title = stringResource(kind.titleRes),
                summary = stringResource(kind.summaryRes),
                checked = notifications.isEnabled(kind),
                onCheckedChange = { onChange(kind, it) },
            )
        }
    }
}

@get:StringRes
private val NotificationKind.titleRes: Int
    get() = when (this) {
        NotificationKind.PROGRESS -> R.string.settings_notifications_progress
        NotificationKind.ALERTS -> R.string.settings_notifications_alerts
        NotificationKind.REMINDERS -> R.string.settings_notifications_reminders
        NotificationKind.MINOR_UPDATES -> R.string.settings_notifications_minor
        NotificationKind.DIGEST -> R.string.settings_notifications_digest
        NotificationKind.SYNC_PROBLEMS -> R.string.settings_notifications_sync
    }

@get:StringRes
private val NotificationKind.summaryRes: Int
    get() = when (this) {
        NotificationKind.PROGRESS -> R.string.settings_notifications_progress_summary
        NotificationKind.ALERTS -> R.string.settings_notifications_alerts_summary
        NotificationKind.REMINDERS -> R.string.settings_notifications_reminders_summary
        NotificationKind.MINOR_UPDATES -> R.string.settings_notifications_minor_summary
        NotificationKind.DIGEST -> R.string.settings_notifications_digest_summary
        NotificationKind.SYNC_PROBLEMS -> R.string.settings_notifications_sync_summary
    }
