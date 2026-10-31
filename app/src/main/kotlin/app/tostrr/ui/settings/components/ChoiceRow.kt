package app.tostrr.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.theme.TostrrColors

/**
 * A setting with a few possible values: shows its [summary] and the selected value, and a tap opens a dialog to pick
 * another. Greyed out and inert when not [enabled].
 */
@Composable
internal fun <T> ChoiceRow(
    title: String,
    summary: String,
    selected: T,
    options: List<T>,
    label: (T) -> String,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val content = if (enabled) TostrrColors.OnSurface else TostrrColors.Muted
    ListItem(
        headlineContent = { Text(title, color = content) },
        supportingContent = {
            Column {
                Text(summary, color = TostrrColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // The current value, under the description as in the system settings.
                Text(
                    label(selected),
                    Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) TostrrColors.Accent else TostrrColors.Muted,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { choosing = true },
    )

    if (choosing && enabled) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            containerColor = TostrrColors.SurfaceHigh,
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        choosing = false
                                        onSelect(option)
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == selected, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
