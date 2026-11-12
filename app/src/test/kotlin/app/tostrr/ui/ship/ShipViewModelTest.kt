package app.tostrr.ui.ship

import app.tostrr.domain.shipping.GeoPosition
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.shipping.TimelineEntry
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.watch
import app.tostrr.ui.navigation.Screen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ShipViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun ship(name: String, done: Boolean = false) = Ship(
        name, listOf("NANGANG", "SINGAPORE", "ZEEBRUGGE"), "ZEEBRUGGE", null, GeoPosition(-30.0, 37.0), 240.0, listOf("052"), done, progress = 0.4,
    )

    private val graph = TestGraph().apply {
        orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EU8VC052692"))
        ships.snapshot.value = ShipSnapshot(1L, listOf(ship("GLOVIS LIGHTHOUSE"), ship("CMA CGM MONACO")))
    }

    @Test fun `opens already showing the ship, among the candidates`() {
        val state = graph.shipViewModel(Screen.Ship("RN1", "CMA CGM MONACO")).state.value

        assertEquals("CMA CGM MONACO", state.voyage?.ship?.name)
        assertEquals(1, state.candidateIndex)
        assertEquals(2, state.candidates)
        assertEquals("052", state.vinBlock)
        assertEquals(TimelineEntry.AtSea(0.4), state.timeline[2])
    }

    @Test fun `lets the ship go once it no longer may carry the car`() = runTest {
        val viewModel = graph.shipViewModel(Screen.Ship("RN1", "GLOVIS LIGHTHOUSE"))
        watch(viewModel)

        graph.ships.snapshot.value = ShipSnapshot(2L, listOf(ship("GLOVIS LIGHTHOUSE", done = true), ship("CMA CGM MONACO")))

        assertNull(viewModel.state.value.voyage)
    }
}
