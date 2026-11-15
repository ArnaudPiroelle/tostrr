package app.tostrr.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.BuildConfig
import app.tostrr.R
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateCheck
import app.tostrr.ui.theme.TostrrColors

/** The installed version, and the button to look for a newer one right away. */
@Composable
internal fun AppSection(checking: Boolean, result: UpdateCheck?, update: AppUpdate?, onCheck: () -> Unit) {
    val installed = BuildConfig.VERSION_NAME
    val summary = when {
        checking -> stringResource(R.string.settings_check_update_checking)
        result == UpdateCheck.AVAILABLE && update != null -> stringResource(R.string.settings_update_available, update.version)
        result == UpdateCheck.UP_TO_DATE -> stringResource(R.string.settings_check_update_up_to_date, installed)
        result == UpdateCheck.FAILED -> stringResource(R.string.settings_check_update_failed)
        else -> stringResource(R.string.settings_check_update_installed, installed)
    }
    SettingsSection(stringResource(R.string.settings_app_section)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_check_update)) },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result == UpdateCheck.FAILED && !checking) MaterialTheme.colorScheme.error else TostrrColors.Muted,
                )
            },
            trailingContent = {
                if (checking) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                }
            },
            colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
            modifier = Modifier.clickable(enabled = !checking, onClick = onCheck),
        )
    }
}
