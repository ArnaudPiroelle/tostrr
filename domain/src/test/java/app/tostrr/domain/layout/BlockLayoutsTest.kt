package app.tostrr.domain.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockLayoutsTest {
    private enum class Block(override val id: String) : LayoutBlock { A("a"), B("b"), C("c"), D("d") }

    private val all = Block.entries
    private fun List<Block>.ids() = map { it.id }

    @Test fun `detail block ids are unique`() {
        assertEquals(DetailBlock.entries.size, DetailBlock.entries.map { it.id }.toSet().size)
    }

    @Test fun `nothing saved means the default order`() {
        assertEquals(listOf("a", "b", "c", "d"), BlockLayouts.visible(all, BlockLayout()).ids())
    }

    @Test fun `the saved order is followed and unknown ids are dropped`() {
        val layout = BlockLayout(order = listOf("c", "gone", "a", "b", "d"))
        assertEquals(listOf("c", "a", "b", "d"), BlockLayouts.resolved(all, layout).ids())
    }

    @Test fun `a block added since goes before its next default neighbour`() {
        // "b" didn't exist when this order was saved.
        val layout = BlockLayout(order = listOf("d", "a", "c"))
        assertEquals(listOf("d", "a", "b", "c"), BlockLayouts.resolved(all, layout).ids())
        // With no later neighbour saved, it goes at the end.
        assertEquals(listOf("c", "a", "b", "d"), BlockLayouts.resolved(all, BlockLayout(order = listOf("c", "a", "b"))).ids())
    }

    @Test fun `hidden blocks are left out of the visible ones`() {
        val layout = BlockLayout(hidden = listOf("b"))
        assertEquals(listOf("a", "c", "d"), BlockLayouts.visible(all, layout).ids())
        assertEquals(listOf("b"), BlockLayouts.hidden(all, layout).ids())
    }

    @Test fun `moving a block swaps it with its neighbour`() {
        val moved = BlockLayouts.move(all, BlockLayout(), "c", -1)
        assertEquals(listOf("a", "c", "b", "d"), BlockLayouts.visible(all, moved).ids())
    }

    @Test fun `moving past either end or an unknown block changes nothing`() {
        val layout = BlockLayout()
        assertEquals(layout, BlockLayouts.move(all, layout, "a", -1))
        assertEquals(layout, BlockLayouts.move(all, layout, "d", 1))
        assertEquals(layout, BlockLayouts.move(all, layout, "nope", 1))
    }

    @Test fun `moving skips hidden blocks, which keep their place after the shown ones`() {
        val layout = BlockLayout(hidden = listOf("b"))
        val moved = BlockLayouts.move(all, layout, "c", -1)
        assertEquals(listOf("c", "a", "d"), BlockLayouts.visible(all, moved).ids())
        assertEquals(listOf("b"), moved.hidden)
    }

    @Test fun `toggling hides then shows a block again`() {
        val hidden = BlockLayouts.toggle(all, BlockLayout(), "b")
        assertEquals(listOf("b"), hidden.hidden)
        assertTrue(BlockLayouts.toggle(all, hidden, "b").isDefault)
    }

    @Test fun `going back to the default order gives the empty layout`() {
        val moved = BlockLayouts.move(all, BlockLayout(), "b", 1)
        assertTrue(BlockLayouts.move(all, moved, "b", -1).isDefault)
    }
}
