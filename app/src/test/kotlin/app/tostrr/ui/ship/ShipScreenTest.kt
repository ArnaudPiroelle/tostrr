package app.tostrr.ui.ship

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import app.tostrr.domain.shipping.GeoPosition
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ShipScreenTest {
    @get:Rule val compose = createComposeRule()

    private var closed = 0

    private fun ship(atBerth: Boolean = false) = Ship(
        name = "GLOVIS LIGHTHOUSE",
        route = listOf("NANGANG", "SINGAPORE", "MAURITIUS", "ZEEBRUGGE"),
        nextPort = if (atBerth) "ZEEBRUGGE" else "MAURITIUS",
        eta = LocalDate.now().plusDays(5),
        position = GeoPosition(-30.75, 37.62),
        headingDeg = 240.0,
        vinBlocks = listOf("052"),
        done = false,
        operator = "Hyundai Glovis",
        atBerth = atBerth,
        progress = 0.19,
        draughtMeters = 10.0,
    )

    private val graph = TestGraph().apply {
        orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EU8VC052692"))
        ships.snapshot.value = ShipSnapshot(1L, listOf(ship()))
    }

    private fun show() = compose.setContent {
        val key = Screen.Ship("RN1", "GLOVIS LIGHTHOUSE")
        TostrrTheme { ShipScreen(key, onClose = { closed++ }, viewModel = graph.shipViewModel(key)) }
    }

    /** Pulls the bottom sheet up from its header, as it only peeks at first. */
    private fun pullUpSheet() {
        compose.onNodeWithText("Glovis Lighthouse").performTouchInput { swipeUp(startY = centerY, endY = centerY - 1200f, durationMillis = 600) }
        compose.waitForIdle()
    }

    @Test fun `shows the ship, where it heads and its route`() {
        show()
        compose.onNodeWithText("Glovis Lighthouse").assertIsDisplayed()

        pullUpSheet()

        compose.onNodeWithText("Hyundai Glovis · Nangang → Zeebrugge via Singapore, Mauritius").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Arrivée prévue à Mauritius").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dans 5 jours").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Puis Zeebrugge").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("En mer").assertExists()
        compose.onNodeWithText("En mer actuellement").performScrollTo().assertIsDisplayed()
        // The percent sign follows a narrow no-break space in French.
        compose.onNodeWithText("du trajet", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("10,0 m").performScrollTo().assertIsDisplayed()
    }

    @Test fun `a ship in port says so`() {
        graph.ships.snapshot.value = ShipSnapshot(1L, listOf(ship(atBerth = true)))
        show()
        pullUpSheet()

        compose.onNodeWithText("À quai à Zeebrugge").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("à quai").performScrollTo().assertIsDisplayed()
    }

    @Test fun `closes, and closes by itself once the ship no longer carries the car`() {
        show()
        compose.onNodeWithContentDescription("Retour").performClick()
        assertEquals(1, closed)

        graph.ships.snapshot.value = ShipSnapshot(2L, emptyList())
        compose.waitForIdle()
        assertEquals(2, closed)
    }
}
