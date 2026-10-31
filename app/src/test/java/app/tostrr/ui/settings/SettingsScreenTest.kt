package app.tostrr.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.testing.TestGraph
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph()

    private fun show() = compose.setContent {
        TostrrTheme { SettingsScreen(onBack = {}, viewModel = graph.settingsViewModel()) }
    }

    /** The account section is the last one, below the fold of the test screen. */
    private fun scrollToAccount() = compose.onNode(hasScrollAction()).performScrollToNode(hasText("Compte Tesla"))

    @Test fun `shows who is logged in`() {
        graph.account.account.value = TeslaAccount("Ada Lovelace")
        show()
        scrollToAccount()

        compose.onNodeWithText("Connecté en tant que").assertIsDisplayed()
        compose.onNodeWithText("Ada Lovelace").assertIsDisplayed()
    }

    @Test fun `without a profile, only the logout button is in the account section`() {
        show()
        scrollToAccount()

        compose.onNodeWithText("Compte Tesla").assertIsDisplayed()
        compose.onNodeWithText("Connecté en tant que").assertDoesNotExist()
        compose.onNodeWithText("Se déconnecter").assertIsDisplayed()
    }

    @Test fun `the theme follows the phone until another is picked`() {
        show()
        compose.onNodeWithText("Auto").assertIsSelected()

        compose.onNodeWithText("Sombre").performClick()

        compose.onNodeWithText("Sombre").assertIsSelected()
        compose.onNodeWithText("Auto").assertIsNotSelected()
        assertEquals(ThemeMode.DARK, graph.appearance.themeMode.value)
    }

    @Test fun `the frequency is picked in a dialog and rescheduled`() {
        show()
        compose.onNodeWithText("Toutes les 15 minutes").assertIsDisplayed()

        compose.onNodeWithText("Fréquence").performClick()
        compose.onNodeWithText("Toutes les 2 heures").performClick()

        compose.onNodeWithText("Toutes les 2 heures").assertIsDisplayed()
        compose.onNodeWithText("Toutes les 30 minutes").assertDoesNotExist()
        assertEquals(listOf<SyncInterval?>(SyncInterval.HOURS_2), graph.scheduler.calls)
    }

    @Test fun `with auto sync off the frequency stays shown but can't be changed`() {
        show()

        compose.onNodeWithText("Actualiser en arrière-plan").performClick()

        compose.onNodeWithText("Seulement quand vous actualisez. Widgets et notifications figés.").assertIsDisplayed()
        compose.onNodeWithText("Fréquence").assertIsDisplayed().performClick()
        compose.onNodeWithText("Toutes les 30 minutes").assertDoesNotExist()
        assertEquals(listOf<SyncInterval?>(null), graph.scheduler.calls)
    }

    @Test fun `logging out asks first`() {
        show()
        scrollToAccount()

        compose.onNodeWithText("Se déconnecter").performClick()
        compose.onNodeWithText("Se déconnecter ?").assertIsDisplayed()
        compose.onNodeWithText("Annuler").performClick()
        assertTrue(graph.auth.loggedIn.value)

        compose.onNodeWithText("Se déconnecter").performClick()
        compose.onNode(hasText("Se déconnecter") and hasAnyAncestor(isDialog())).performClick()
        assertFalse(graph.auth.loggedIn.value)
    }
}
