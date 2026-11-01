package app.tostrr.ui.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tostrr.domain.order.Order
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrdersScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph()
    private val opened = mutableListOf<Order>()
    private var settingsOpened = false

    private fun show() = compose.setContent {
        TostrrTheme {
            OrdersScreen(onOpen = { opened += it }, onOpenSettings = { settingsOpened = true }, viewModel = graph.ordersViewModel())
        }
    }

    @Test fun `lists the orders and opens one`() {
        graph.orders.orders.value = listOf(order("RN100000001"))
        show()

        compose.onNodeWithText("#RN100000001").assertIsDisplayed().performClick()

        assertEquals(listOf("RN100000001"), opened.map { it.reference })
    }

    @Test fun `says when there is no order`() {
        show()

        compose.onNodeWithText("Aucune commande en cours.").assertIsDisplayed()
    }

    @Test fun `shows a loading state while the first orders are fetched`() {
        graph.orders.syncing.value = true
        show()

        compose.onNodeWithText("Récupération de vos commandes…").assertIsDisplayed()
        compose.onNodeWithText("Aucune commande en cours.").assertDoesNotExist()

        graph.orders.orders.value = listOf(order("RN100000001"))
        graph.orders.syncing.value = false
        compose.onNodeWithText("Récupération de vos commandes…").assertDoesNotExist()
        compose.onNodeWithText("#RN100000001").assertIsDisplayed()
    }

    @Test fun `a failed refresh shows why`() {
        graph.orders.onRefresh = { throw IllegalStateException("boom") }
        show()

        compose.onNodeWithContentDescription("Actualiser").performClick()

        compose.onNodeWithText("Synchronisation impossible : boom").assertIsDisplayed()
        assertEquals(1, graph.orders.refreshes)
    }

    @Test fun `the gear opens the settings`() {
        show()

        compose.onNodeWithContentDescription("Réglages").performClick()

        assertTrue(settingsOpened)
    }
}
