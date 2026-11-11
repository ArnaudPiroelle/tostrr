package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.history.ChangeCategory
import app.tostrr.domain.history.HistoryFilter
import app.tostrr.domain.order.Change
import app.tostrr.ui.theme.TostrrColors

/** The order's changes, newest first, as [filter] leaves them ([shown]). */
@Composable
internal fun HistoryContent(
    history: List<Change>,
    filter: HistoryFilter,
    shown: List<Change>,
    onToggleImportantOnly: () -> Unit,
    onSelectCategory: (ChangeCategory?) -> Unit,
) {
    if (history.isEmpty()) return Empty(stringResource(R.string.detail_history_empty))
    val counts = remember(history, filter.importantOnly) { filter.categoryCounts(history) }
    HistoryFilters(filter, counts, onToggleImportantOnly, onSelectCategory)
    Spacer(Modifier.height(8.dp))
    if (shown.isEmpty()) return Empty(stringResource(R.string.detail_history_no_match))
    shown.take(MAX_HISTORY_ROWS).forEachIndexed { i, change ->
        if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = TostrrColors.Border)
        ChangeRow(change)
    }
    if (shown.size > MAX_HISTORY_ROWS) {
        Text(
            pluralStringResource(R.plurals.detail_history_more, shown.size - MAX_HISTORY_ROWS, shown.size - MAX_HISTORY_ROWS),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TostrrColors.Muted,
        )
    }
}

private const val MAX_HISTORY_ROWS = 100
