package app.tostrr.ui.detail

import android.content.ClipboardManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph().apply {
        orders.orders.value = listOf(order("RN1", deliveryCenter = "Tesla Paris-Ouest"))
        deliveryCenters += DeliveryCenter("Tesla Paris-Ouest", "FR", "Paris", "1 rue de Paris")
    }

    private fun show(reference: String = "RN1") = compose.setContent {
        TostrrTheme { DetailScreen(reference, onOpenChecklist = {}, onBack = {}, viewModel = graph.detailViewModel(reference)) }
    }

    @Test fun `shows the order and its delivery centre`() {
        show()

        // In the header, and in the top bar that fades in on scroll.
        compose.onAllNodesWithText("Commande #RN1").assertCountEquals(2)
        compose.onNodeWithText("Tesla Paris-Ouest").performScrollTo().assertIsDisplayed()
    }

    @Test fun `a collapsed section hides its content and is remembered`() {
        show()

        compose.onNodeWithText("Informations de livraison").performScrollTo().performClick()

        compose.onNodeWithText("Tesla Paris-Ouest").assertDoesNotExist()
        assertEquals(listOf("delivery"), graph.layout.layout.value.collapsed)
    }

    @Test fun `the top bar opens the arrangement sheet`() {
        show()

        compose.onNodeWithContentDescription("Organiser la page").performClick()

        compose.onNodeWithText("Réinitialiser").assertIsDisplayed()
    }

    @Test fun `the VIN can be copied`() {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EK0PC000001"))
        show()

        compose.onNodeWithContentDescription("Copier le VIN").performClick()
        compose.waitForIdle()

        val clipboard = RuntimeEnvironment.getApplication().getSystemService(ClipboardManager::class.java)
        assertEquals("LRW3E7EK0PC000001", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test fun `an unknown VIN has nothing to copy`() {
        show()

        compose.onNodeWithText("VIN: inconnu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Copier le VIN").assertDoesNotExist()
    }

    @Test fun `an unknown order shows nothing rather than crashing`() {
        show("RN404")

        compose.onAllNodesWithText("Commande #RN404").assertCountEquals(0)
    }
}
