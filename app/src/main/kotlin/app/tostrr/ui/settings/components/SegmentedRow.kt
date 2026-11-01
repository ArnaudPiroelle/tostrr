package app.tostrr.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

/**
 * A setting with a few short values, all shown at once under its [summary] as segmented buttons, each with its [icon]
 * (kept when selected: the filled background marks the choice).
 */
@Composable
internal fun <T> SegmentedRow(
    title: String,
    summary: String,
    selected: T,
    options: List<T>,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
) {
    Column {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(summary, color = TostrrColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = { Icon(icon(option), contentDescription = null, Modifier.size(SegmentedButtonDefaults.IconSize)) },
                    label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}
