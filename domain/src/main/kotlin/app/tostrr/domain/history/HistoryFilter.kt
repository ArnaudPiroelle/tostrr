package app.tostrr.domain.history

import app.tostrr.domain.order.Change

/**
 * Which changes of an order's history to show: only the [important][ChangeCategory.important] ones, and only one
 * [category]. The default shows them all.
 */
data class HistoryFilter(
    val importantOnly: Boolean = false,
    val category: ChangeCategory? = null,
) {
    fun apply(changes: List<Change>): List<Change> = changes.filter { change ->
        (!importantOnly || change.category.important) && (category == null || change.category == category)
    }

    /** How many of [changes] each category has, among those [importantOnly] leaves, in the categories' order. */
    fun categoryCounts(changes: List<Change>): Map<ChangeCategory, Int> =
        changes.map { it.category }
            .filter { !importantOnly || it.important }
            .groupingBy { it }.eachCount()
            .toSortedMap()
}
