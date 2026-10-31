package app.tostrr.ui.orders

import app.tostrr.domain.error.ApiException
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.SessionExpiredException
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.watch
import app.tostrr.ui.common.UiError
import app.tostrr.ui.orders.OrdersViewModel.Action
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()

    @Test fun `the cached orders show at once, without fetching`() {
        graph.orders.orders.value = listOf(order("RN1"), order("RN2"))

        val state = graph.ordersViewModel().state.value

        assertEquals(listOf("RN1", "RN2"), state.orders.map { it.reference })
        assertEquals(0, graph.orders.refreshes)
    }

    @Test fun `an empty list is loading while orders are being fetched elsewhere`() = runTest {
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        graph.orders.syncing.value = true
        assertTrue(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.refreshing)

        graph.orders.orders.value = listOf(order())
        assertFalse(viewModel.state.value.loading)

        graph.orders.syncing.value = false
        assertFalse(viewModel.state.value.refreshing)
    }

    @Test fun `refreshing shows the new orders`() = runTest {
        val pull = CompletableDeferred<Unit>()
        graph.orders.onRefresh = {
            pull.await()
            graph.orders.orders.value = listOf(order("RN1"))
        }
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnRefresh)
        assertTrue(viewModel.state.value.refreshing)
        pull.complete(Unit)

        assertFalse(viewModel.state.value.refreshing)
        assertEquals(listOf("RN1"), viewModel.state.value.orders.map { it.reference })
    }

    @Test fun `a second refresh within the cooldown is ignored`() = runTest {
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnRefresh)
        viewModel.handle(Action.OnRefresh)

        assertEquals(1, graph.orders.refreshes)
    }

    @Test fun `an API error keeps its HTTP code`() = runTest {
        graph.orders.onRefresh = { throw ApiException(500) }
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnRefresh)

        assertEquals(UiError.Api(500), viewModel.state.value.sync.error)
    }

    @Test fun `any other failure is explained`() = runTest {
        graph.orders.onRefresh = { throw IllegalStateException("boom") }
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnRefresh)

        assertEquals(UiError.Unexpected("boom"), viewModel.state.value.sync.error)
    }

    @Test fun `an expired session logs out`() = runTest {
        graph.orders.orders.value = listOf(order())
        graph.orders.onRefresh = { throw SessionExpiredException() }
        val viewModel = graph.ordersViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnRefresh)

        assertFalse(graph.auth.loggedIn.value)
        assertEquals(emptyList<Any>(), viewModel.state.value.orders)
        assertEquals(UiError.Auth(AuthError.SESSION_EXPIRED), viewModel.state.value.sync.error)
    }
}
