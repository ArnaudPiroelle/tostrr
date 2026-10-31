package app.tostrr.ui.login

import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.testing.FakeAuthRepository
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.sideEffects
import app.tostrr.ui.common.UiError
import app.tostrr.ui.login.LoginViewModel.Action
import app.tostrr.ui.login.LoginViewModel.SideEffect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `logging in opens Tesla's login page`() = runTest {
        val viewModel = TestGraph().loginViewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnLogin)

        assertEquals(listOf(SideEffect.OpenLoginPage(FakeAuthRepository.LOGIN_URL)), effects)
    }

    @Test fun `the redirect opens the session, schedules the sync and fetches the orders once`() = runTest {
        val graph = TestGraph(appScope = this).apply { auth.loggedIn.value = false }
        val viewModel = graph.loginViewModel()

        viewModel.handle(Action.OnRedirect("tesla://auth/callback?code=1&state=2"))
        testScheduler.advanceUntilIdle()

        assertTrue(graph.auth.loggedIn.value)
        assertEquals(listOf<SyncInterval?>(SyncInterval.DEFAULT), graph.scheduler.calls)
        assertEquals(1, graph.orders.refreshes)
        assertFalse(viewModel.state.value.busy)
        assertNull(viewModel.state.value.error)
    }

    @Test fun `a rejected redirect shows why`() = runTest {
        val graph = TestGraph().apply {
            auth.loggedIn.value = false
            auth.loginFailure = AuthException(AuthError.INVALID_STATE)
        }
        val viewModel = graph.loginViewModel()

        viewModel.handle(Action.OnRedirect("tesla://auth/callback?code=1&state=bad"))

        assertEquals(UiError.Auth(AuthError.INVALID_STATE), viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)
        assertEquals(0, graph.orders.refreshes)
    }

    @Test fun `any other failure is explained`() = runTest {
        val graph = TestGraph().apply {
            auth.loggedIn.value = false
            auth.loginFailure = IllegalStateException("timeout")
        }
        val viewModel = graph.loginViewModel()

        viewModel.handle(Action.OnRedirect("tesla://auth/callback?code=1"))

        assertEquals(UiError.Unexpected("timeout"), viewModel.state.value.error)
    }

    @Test fun `a redirect delivered again once logged in is ignored`() = runTest {
        val graph = TestGraph()
        val viewModel = graph.loginViewModel()

        viewModel.handle(Action.OnRedirect("tesla://auth/callback?code=1"))

        assertEquals(emptyList<String>(), graph.auth.redirects)
    }
}
