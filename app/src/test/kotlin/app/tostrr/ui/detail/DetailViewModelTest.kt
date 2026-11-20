package app.tostrr.ui.detail

import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.history.ChangeCategory
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.order.Change
import app.tostrr.domain.shipping.GeoPosition
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.vin.VinCode
import app.tostrr.domain.vin.VinSection
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.sideEffects
import app.tostrr.testing.watch
import app.tostrr.ui.detail.DetailViewModel.Action
import app.tostrr.ui.detail.DetailViewModel.SideEffect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class DetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val center = DeliveryCenter("Tesla Paris-Ouest", "FR", "Paris", "1 rue de Paris")
    private val change = Change(1L, "status", "BOOKED", "IN_PRODUCTION")
    private val graph = TestGraph().apply {
        orders.orders.value = listOf(order("RN1"), order("RN2", deliveryCenter = null))
        orders.history.value = mapOf("RN1" to listOf(change), "RN2" to emptyList())
        deliveryCenters += center
    }

    @Test fun `exporting writes the order with its history, then shares the file`() = runTest {
        val viewModel = graph.detailViewModel("RN1")
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnExport)

        val (reference, json) = graph.exports.written.single()
        assertEquals("RN1", reference)
        assertTrue(json.contains("\"path\": \"status\""))
        val path = (effects.single() as SideEffect.ShareExport).path
        assertEquals(json, File(path).readText())
    }

    @Test fun `an order no longer known has nothing to export`() = runTest {
        val viewModel = graph.detailViewModel("RN404")
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnExport)

        assertTrue(graph.exports.written.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test fun `opens already showing the order`() {
        val state = graph.detailViewModel("RN1").state.value

        assertEquals("RN1", state.order?.reference)
        assertEquals(listOf(change), state.history)
    }

    @Test fun `finds the order's delivery centre`() = runTest {
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)

        assertEquals(center, viewModel.state.value.deliveryCenter)
    }

    @Test fun `decodes the order's VIN once it has one`() = runTest {
        graph.vinCodes.know(VinCode(VinSection.FACTORY, "C", mapOf("en" to "Shanghai")))
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)
        assertNull(viewModel.state.value.vinDecoding)

        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EB5PC000003"))

        val decoding = viewModel.state.value.vinDecoding!!
        assertEquals("LRW3E7EB5PC000003", decoding.vin)
        assertEquals("Shanghai", decoding.segments.single { it.section == VinSection.FACTORY }.meaning?.values?.get("en"))
    }

    @Test fun `an order gone from the list leaves nothing to show`() = runTest {
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)

        graph.orders.orders.value = emptyList()

        assertNull(viewModel.state.value.order)
    }

    @Test fun `the history opens on the important changes, and filters by category`() = runTest {
        val vin = Change(3L, "order.vin", null, "LRWYGCEK1TC000001")
        val contract = Change(2L, "details.tasks.agreements.eSignStatus", "COMPLETE", "VOIDED")
        val insurance = Change(1L, "details.tasks.insurance.hasVin", "true", "false")
        graph.orders.history.value = mapOf("RN1" to listOf(vin, contract, insurance))
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)

        assertEquals(listOf(vin, contract), viewModel.state.value.shownHistory)

        viewModel.handle(Action.OnSelectHistoryCategory(ChangeCategory.AGREEMENTS))
        assertEquals(listOf(contract), viewModel.state.value.shownHistory)

        viewModel.handle(Action.OnToggleImportantOnly)
        assertNull(viewModel.state.value.historyFilter.category)
        assertEquals(listOf(vin, contract, insurance), viewModel.state.value.shownHistory)
    }

    @Test fun `follows the ship carrying the car, by its VIN block`() = runTest {
        graph.orders.orders.value = listOf(order("RN1").copy(vin = "LRW3E7EU8VC052692"))
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)
        assertNull(viewModel.state.value.voyage)

        graph.ships.snapshot.value = ShipSnapshot(
            1L,
            listOf(
                Ship("GLOVIS LIGHTHOUSE", listOf("NANGANG", "ZEEBRUGGE"), "ZEEBRUGGE", null, GeoPosition(-30.0, 37.0), 240.0, listOf("052"), done = false),
            ),
        )

        assertEquals(listOf("GLOVIS LIGHTHOUSE"), viewModel.state.value.voyage?.candidates?.map { it.ship.name })
        // Built by the ViewModel, not while the globe composes: one lane per candidate.
        assertEquals(1, viewModel.state.value.voyageLanes.size)
        assertTrue(viewModel.state.value.voyageLanes.single().ahead.isNotEmpty())
    }

    @Test fun `collapsing a section is saved for every order`() = runTest {
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)

        viewModel.handle(Action.OnToggleBlockExpanded(DetailBlock.DELIVERY))

        assertEquals(listOf("delivery"), viewModel.state.value.layout.collapsed)
        assertEquals(listOf("delivery"), graph.layout.layout.value.collapsed)
    }

    @Test fun `hiding a block then resetting brings it back`() = runTest {
        val viewModel = graph.detailViewModel("RN1")
        watch(viewModel)

        viewModel.handle(Action.OnToggleBlock(DetailBlock.HISTORY))
        assertEquals(listOf("history"), viewModel.state.value.layout.hidden)

        viewModel.handle(Action.OnResetLayout)
        assertEquals(emptyList<String>(), viewModel.state.value.layout.hidden)
    }
}
