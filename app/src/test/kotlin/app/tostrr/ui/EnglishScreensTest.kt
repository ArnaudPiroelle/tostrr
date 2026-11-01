package app.tostrr.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.error.ApiException
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.ui.checklist.ChecklistScreen
import app.tostrr.ui.orders.OrdersScreen
import app.tostrr.ui.settings.SettingsScreen
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The other language the app ships: the default resources, for any device not set to French. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class EnglishScreensTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph()

    @Test fun `settings`() {
        graph.account.account.value = TeslaAccount("Ada Lovelace")
        compose.setContent { TostrrTheme { SettingsScreen(onBack = {}, viewModel = graph.settingsViewModel()) } }

        compose.onNodeWithText("Signed in as").assertIsDisplayed()
        compose.onNodeWithText("Sign out").assertIsDisplayed()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Dark").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Every 15 minutes"))
        compose.onNodeWithText("Automatic refresh").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Daily summary"))
        compose.onNodeWithText("Delivery reminders").assertIsDisplayed()
    }

    @Test fun `orders, their steps and a refresh error`() {
        graph.orders.orders.value = listOf(order("RN1"))
        graph.orders.onRefresh = { throw ApiException(503) }
        compose.setContent { TostrrTheme { OrdersScreen(onOpen = {}, onOpenSettings = {}, viewModel = graph.ordersViewModel()) } }

        compose.onNodeWithText("Order placed").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.onNodeWithText("Tesla API error (HTTP 503).").assertIsDisplayed()
    }

    @Test fun `checklist questions`() {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, null, 1, emptyMap()))
        compose.setContent {
            TostrrTheme {
                ChecklistScreen("RN1", onClose = {}, viewModel = graph.checklistViewModel("RN1", RuntimeEnvironment.getApplication()))
            }
        }

        // The section, as the tag above the question.
        compose.onNodeWithText("EXTERIOR").assertExists()
        compose.onNodeWithText("Does the VIN on the windscreen match the registration certificate?").performScrollTo().assertIsDisplayed()
    }
}
