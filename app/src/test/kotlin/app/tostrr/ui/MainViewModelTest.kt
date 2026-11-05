package app.tostrr.ui

import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.update.AppUpdate
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.watch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()

    @Test fun `the session is known before the first frame, so the right screen opens`() {
        graph.auth.loggedIn.value = true
        assertTrue(graph.mainViewModel().state.value.loggedIn)

        graph.auth.loggedIn.value = false
        assertFalse(graph.mainViewModel().state.value.loggedIn)
    }

    @Test fun `follows logouts and the orders a deep link can open`() = runTest {
        graph.orders.orders.value = listOf(order("RN1"))
        val viewModel = graph.mainViewModel()
        watch(viewModel)
        assertEquals(setOf("RN1"), viewModel.state.value.orderReferences)

        graph.orders.orders.value = listOf(order("RN1"), order("RN2"))
        graph.auth.logout()

        assertEquals(setOf("RN1", "RN2"), viewModel.state.value.orderReferences)
        assertFalse(viewModel.state.value.loggedIn)
    }

    @Test fun `looks for an update once watched and follows what is offered`() = runTest {
        val viewModel = graph.mainViewModel()
        assertEquals(0, graph.updates.checks)
        watch(viewModel)
        assertEquals(1, graph.updates.checks)
        assertNull(viewModel.state.value.availableUpdate)

        graph.updates.updateToOffer.value = AppUpdate("0.2.0", "", "https://example.com/a.apk", 1)

        assertEquals("0.2.0", viewModel.state.value.availableUpdate)
    }

    @Test fun `follows the chosen theme`() = runTest {
        graph.appearance.themeMode.value = ThemeMode.LIGHT
        val viewModel = graph.mainViewModel()
        assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)
        watch(viewModel)

        graph.appearance.themeMode.value = ThemeMode.DARK

        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
    }
}
