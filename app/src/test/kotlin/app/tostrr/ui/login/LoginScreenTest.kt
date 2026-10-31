package app.tostrr.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.testing.TestGraph
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph().apply { auth.loggedIn.value = false }

    @Test fun `the redirect from the deep link is handed to the login once`() {
        var handled = false
        compose.setContent {
            TostrrTheme {
                LoginScreen(
                    redirect = "tesla://auth/callback?code=1",
                    onRedirectHandled = { handled = true },
                    viewModel = graph.loginViewModel(),
                )
            }
        }

        compose.waitForIdle()
        assertTrue(handled)
        assertTrue(graph.auth.loggedIn.value)
        assertEquals(listOf("tesla://auth/callback?code=1"), graph.auth.redirects)
    }

    @Test fun `a pasted redirect that is rejected shows why`() {
        graph.auth.loginFailure = AuthException(AuthError.INVALID_STATE)
        compose.setContent {
            TostrrTheme { LoginScreen(redirect = null, onRedirectHandled = {}, viewModel = graph.loginViewModel()) }
        }

        compose.onNodeWithText("La redirection ne fonctionne pas ?").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("tesla://auth/callback?code=1&state=bad")
        compose.onNodeWithText("Valider").performClick()

        compose.onNodeWithText("État OAuth invalide, relancez la connexion.").assertIsDisplayed()
    }
}
