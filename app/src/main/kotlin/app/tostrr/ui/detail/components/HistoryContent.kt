package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Change
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun HistoryContent(history: List<Change>) {
    if (history.isEmpty()) return Empty(stringResource(R.string.detail_history_empty))
    history.take(MAX_HISTORY_ROWS).forEachIndexed { i, change ->
        if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = TostrrColors.Border)
        ChangeRow(change)
    }
    if (history.size > MAX_HISTORY_ROWS) {
        Text(
            pluralStringResource(R.plurals.detail_history_more, history.size - MAX_HISTORY_ROWS, history.size - MAX_HISTORY_ROWS),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TostrrColors.Muted,
        )
    }
}

private const val MAX_HISTORY_ROWS = 100
