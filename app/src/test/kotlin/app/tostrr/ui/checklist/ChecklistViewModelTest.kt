package app.tostrr.ui.checklist

import app.tostrr.domain.checklist.Checklist
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.sideEffects
import app.tostrr.testing.watch
import app.tostrr.ui.checklist.ChecklistViewModel.Action
import app.tostrr.ui.checklist.ChecklistViewModel.SideEffect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ChecklistViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()
    private fun viewModel() = graph.checklistViewModel("RN1", RuntimeEnvironment.getApplication())

    @Test fun `no report until the checklist is locked`() = runTest {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnShareReport)

        assertTrue(effects.isEmpty())
        assertTrue(graph.reports.written.isEmpty())
    }

    @Test fun `a locked checklist's report is written for its order, to open or to share`() = runTest {
        graph.orders.orders.value = listOf(order("RN1"))
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnLock)
        viewModel.handle(Action.OnOpenReport)
        viewModel.handle(Action.OnShareReport)

        val (checklist, order) = graph.reports.written.first()
        assertTrue(checklist.locked)
        assertEquals("RN1", order?.reference)
        assertTrue(effects[0] is SideEffect.OpenReport)
        assertTrue(effects[1] is SideEffect.ShareReport)
    }

    @Test fun `a locked checklist has no question being changed`() = runTest {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap(), lockedAtEpochMs = 2L))
        val viewModel = viewModel()
        watch(viewModel)

        viewModel.handle(Action.OnEditQuestion("exterior-01"))

        assertNull(viewModel.state.value.editingQuestionId)
    }
}
