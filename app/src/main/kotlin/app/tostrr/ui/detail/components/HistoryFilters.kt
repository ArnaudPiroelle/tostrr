package app.tostrr.ui.detail.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.history.ChangeCategory
import app.tostrr.domain.history.HistoryFilter

/** "Important only", then one chip per category the history has, with its count. */
@Composable
internal fun HistoryFilters(
    filter: HistoryFilter,
    counts: Map<ChangeCategory, Int>,
    onToggleImportantOnly: () -> Unit,
    onSelectCategory: (ChangeCategory?) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = filter.importantOnly,
            onClick = onToggleImportantOnly,
            label = { Text(stringResource(R.string.detail_history_important)) },
        )
        counts.forEach { (category, count) ->
            val selected = filter.category == category
            FilterChip(
                selected = selected,
                onClick = { onSelectCategory(if (selected) null else category) },
                label = { Text(stringResource(R.string.detail_history_category_count, stringResource(category.labelRes), count)) },
            )
        }
    }
}
