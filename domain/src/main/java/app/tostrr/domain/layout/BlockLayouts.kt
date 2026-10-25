package app.tostrr.domain.layout

/** Applies and edits a [BlockLayout]. Pure functions, shared by every reorderable screen. */
object BlockLayouts {

    /**
     * Every block in the saved order. Unknown ids (blocks since removed) are dropped; a block the saved order
     * doesn't know (added since) goes right before the next default block that is there, else at the end.
     */
    fun <T : LayoutBlock> resolved(all: List<T>, layout: BlockLayout): List<T> {
        val saved = layout.order.mapNotNull { id -> all.firstOrNull { it.id == id } }
        if (saved.isEmpty()) return all
        val result = saved.toMutableList()
        all.forEachIndexed { index, block ->
            if (block !in result) {
                val next = all.drop(index + 1).firstOrNull { it in result }
                if (next != null) result.add(result.indexOf(next), block) else result.add(block)
            }
        }
        return result
    }

    /** The blocks to show, in order. */
    fun <T : LayoutBlock> visible(all: List<T>, layout: BlockLayout): List<T> =
        resolved(all, layout).filter { it.id !in layout.hidden }

    /** The hidden blocks, in their saved order. */
    fun <T : LayoutBlock> hidden(all: List<T>, layout: BlockLayout): List<T> =
        resolved(all, layout).filter { it.id in layout.hidden }

    /** Moves a shown block by [delta] places among the shown ones; out of range or unknown leaves it as it is. */
    fun <T : LayoutBlock> move(all: List<T>, layout: BlockLayout, id: String, delta: Int): BlockLayout {
        val shown = visible(all, layout).toMutableList()
        val from = shown.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in shown.indices) return layout
        shown.add(to, shown.removeAt(from))
        return normalized(all, layout.copy(order = (shown + hidden(all, layout)).map { it.id }))
    }

    /** Hides a shown block or shows a hidden one; it keeps its place in the order. */
    fun <T : LayoutBlock> toggle(all: List<T>, layout: BlockLayout, id: String): BlockLayout {
        if (all.none { it.id == id }) return layout
        val hidden = if (id in layout.hidden) layout.hidden - id else layout.hidden + id
        return normalized(all, BlockLayout(resolved(all, layout).map { it.id }, hidden))
    }

    /** A layout equal to the default becomes the empty one, so "reset" knows there is nothing left to undo. */
    private fun <T : LayoutBlock> normalized(all: List<T>, layout: BlockLayout) =
        if (layout.hidden.isEmpty() && layout.order == all.map { it.id }) BlockLayout() else layout
}
