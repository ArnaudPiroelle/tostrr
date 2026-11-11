package app.tostrr.domain.shipping

import app.tostrr.domain.order.LocalSignals
import app.tostrr.domain.order.OrderMapper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShipVoyagesTest {
    private fun order(vin: String?, inAccountSince: LocalDate? = null) = OrderMapper.map(
        Json.parseToJsonElement(if (vin == null) """{"referenceNumber":"RN1"}""" else """{"referenceNumber":"RN1","vin":"$vin"}""").jsonObject,
        Json.parseToJsonElement("{}").jsonObject,
        0L,
        local = LocalSignals(inAccountSince = inAccountSince),
    )

    private fun ship(name: String, blocks: List<String>, done: Boolean = false) = Ship(
        name = name,
        route = listOf("NANGANG", "SINGAPORE", "MAURITIUS", "ZEEBRUGGE"),
        nextPort = "ZEEBRUGGE",
        eta = LocalDate.of(2026, 11, 28),
        position = GeoPosition(-30.75, 37.62),
        headingDeg = 240.0,
        vinBlocks = blocks,
        done = done,
    )

    private val snapshot = ShipSnapshot(
        1_000L,
        listOf(
            ship("GLOVIS NOVA", listOf("052"), done = true),
            ship("GLOVIS LIGHTHOUSE", listOf("051", "052")),
            ship("CMA CGM MONACO", listOf("052", "053")),
        ),
    )

    @Test fun `the block is the first three digits of the serial`() {
        assertEquals("052", ShipVoyages.vinBlock("LRW3E7EU8VC052692"))
    }

    @Test fun `every sailing ship with the block is a candidate, in the snapshot's order`() {
        val voyage = ShipVoyages.of(order("LRW3E7EU8VC052692"), snapshot)!!
        assertEquals(listOf("GLOVIS LIGHTHOUSE", "CMA CGM MONACO"), voyage.candidates.map { it.ship.name })
        assertTrue(voyage.uncertain)
        assertEquals("052", voyage.vinBlock)
        assertEquals(1_000L, voyage.snapshotAtEpochMs)
    }

    @Test fun `a block only one ship carries is certain`() {
        val voyage = ShipVoyages.of(order("LRW3E7EU8VC051692"), snapshot)!!
        assertEquals(listOf("GLOVIS LIGHTHOUSE"), voyage.candidates.map { it.ship.name })
        assertFalse(voyage.uncertain)
    }

    @Test fun `ports before the next one are passed`() {
        val ports = ShipVoyages.of(order("LRW3E7EU8VC052692"), snapshot)!!.candidates.first().ports
        assertEquals(listOf(true, true, true, false), ports.map { it.passed })
    }

    @Test fun `no voyage without a Shanghai VIN, a ship with its block, or once delivered`() {
        assertNull(ShipVoyages.of(order(null), snapshot))
        assertNull(ShipVoyages.of(order("XP7YGCEK1TB052692"), snapshot))
        assertNull(ShipVoyages.of(order("LRW3E7EU8VC099692"), snapshot))
        assertNull(ShipVoyages.of(order("LRW3E7EU8VC052692", inAccountSince = LocalDate.of(2026, 12, 20)), snapshot))
        assertNull(ShipVoyages.of(order("LRW3E7EU8VC052692"), null))
    }

    @Test fun `only Shanghai VINs are tracked`() {
        assertTrue(ShipVoyages.tracks("LRW3E7EU8VC052692"))
        assertFalse(ShipVoyages.tracks("XP7YGCEK1TB052692"))
        assertFalse(ShipVoyages.tracks(null))
    }
}
