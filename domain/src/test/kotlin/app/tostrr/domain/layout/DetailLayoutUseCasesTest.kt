package app.tostrr.domain.layout

import app.tostrr.domain.layout.usecase.MoveDetailBlockUseCase
import app.tostrr.domain.layout.usecase.ResetDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockExpandedUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailLayoutUseCasesTest {
    private class FakeLayouts : DetailLayoutRepository {
        override val layout = MutableStateFlow(BlockLayout())
        override fun save(layout: BlockLayout) {
            this.layout.value = layout
        }
    }

    private val repository = FakeLayouts()
    private val shown get() = BlockLayouts.visible(DetailBlock.entries, repository.layout.value)

    private val first = DetailBlock.entries.first { !it.hiddenByDefault }
    private val second = DetailBlock.entries.filter { !it.hiddenByDefault }[1]

    @Test fun `moving a block down saves the new order`() {
        MoveDetailBlockUseCase(repository)(first, 1)
        assertEquals(listOf(second, first), shown.take(2))
    }

    @Test fun `hiding a block saves it hidden, and toggling again shows it`() {
        ToggleDetailBlockUseCase(repository)(first)
        assertFalse(first in shown)
        ToggleDetailBlockUseCase(repository)(first)
        assertTrue(first in shown)
    }

    @Test fun `collapsing a block is saved`() {
        ToggleDetailBlockExpandedUseCase(repository)(first)
        assertTrue(first.id in repository.layout.value.collapsed)
    }

    @Test fun `resetting undoes the arrangement but keeps the collapsed blocks`() {
        MoveDetailBlockUseCase(repository)(first, 1)
        ToggleDetailBlockUseCase(repository)(second)
        ToggleDetailBlockExpandedUseCase(repository)(first)
        ResetDetailLayoutUseCase(repository)()
        assertEquals(BlockLayout(collapsed = listOf(first.id)), repository.layout.value)
    }
}
