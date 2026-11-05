package app.tostrr.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.update.AppUpdate
import app.tostrr.ui.theme.TostrrColors

/** The way back to an update left for later. */
@Composable
internal fun UpdateSection(update: AppUpdate, onOpen: () -> Unit) {
    SettingsSection(stringResource(R.string.settings_update_section)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_update_available, update.version)) },
            supportingContent = {
                Text(
                    stringResource(R.string.settings_update_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = TostrrColors.Muted,
                )
            },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
            modifier = Modifier.clickable(onClick = onOpen),
        )
    }
}
