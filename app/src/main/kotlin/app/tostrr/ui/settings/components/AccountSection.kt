package app.tostrr.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun AccountSection(account: TeslaAccount?, onLogout: () -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    SettingsSection(stringResource(R.string.settings_account_section)) {
        // Who is logged in, when Tesla gave the account's name.
        if (account != null) {
            ListItem(
                overlineContent = { Text(stringResource(R.string.settings_account_logged_in_as), style = MaterialTheme.typography.bodySmall, color = TostrrColors.Muted) },
                headlineContent = { Text(account.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Avatar(account.avatarPath) },
                colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
            )
        }
        // An action, not a setting: a plain button.
        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = if (account != null) 0.dp else 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.settings_logout))
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = TostrrColors.SurfaceHigh,
            title = { Text(stringResource(R.string.settings_logout_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_logout_text),
                    color = TostrrColors.Muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) { Text(stringResource(R.string.settings_logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
