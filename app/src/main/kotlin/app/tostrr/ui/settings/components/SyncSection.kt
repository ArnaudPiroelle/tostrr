package app.tostrr.ui.settings.components

import androidx.annotation.StringRes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun SyncSection(
    sync: SyncSettings,
    onAutoSyncChange: (Boolean) -> Unit,
    onIntervalChange: (SyncInterval) -> Unit,
) {
    SettingsSection(stringResource(R.string.settings_sync_section)) {
        SwitchRow(
            title = stringResource(R.string.settings_sync_auto),
            summary = stringResource(if (sync.autoSync) R.string.settings_sync_auto_on else R.string.settings_sync_auto_off),
            checked = sync.autoSync,
            onCheckedChange = onAutoSyncChange,
        )
        HorizontalDivider(color = TostrrColors.Border)
        // Always shown, so the screen doesn't change shape; only usable while auto sync is on.
        val labels = SyncInterval.entries.associateWith { stringResource(it.labelRes) }
        ChoiceRow(
            title = stringResource(R.string.settings_sync_interval),
            summary = stringResource(R.string.settings_sync_interval_summary),
            selected = sync.interval,
            options = SyncInterval.entries,
            label = labels::getValue,
            enabled = sync.autoSync,
            onSelect = onIntervalChange,
        )
    }
}

@get:StringRes
private val SyncInterval.labelRes: Int
    get() = when (this) {
        SyncInterval.MINUTES_15 -> R.string.settings_sync_interval_15_min
        SyncInterval.MINUTES_30 -> R.string.settings_sync_interval_30_min
        SyncInterval.HOUR_1 -> R.string.settings_sync_interval_1_h
        SyncInterval.HOURS_2 -> R.string.settings_sync_interval_2_h
        SyncInterval.HOURS_6 -> R.string.settings_sync_interval_6_h
    }
