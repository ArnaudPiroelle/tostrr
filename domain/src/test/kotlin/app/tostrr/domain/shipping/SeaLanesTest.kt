package app.tostrr.domain.shipping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeaLanesTest {
    @Test fun `the lane goes through every port, in order`() {
        val lane = SeaLanes.lane(listOf("NANGANG", "SINGAPORE", "MAURITIUS", "ZEEBRUGGE"))
        val ports = listOf("NANGANG", "SINGAPORE", "MAURITIUS", "ZEEBRUGGE").map { SeaLanes.ports.getValue(it) }
        assertEquals(ports.first(), lane.first())
        assertEquals(ports.last(), lane.last())
        assertEquals(ports.map { lane.indexOf(it) }.sorted(), ports.map { lane.indexOf(it) })
    }

    @Test fun `Asia to Europe goes round the Cape, not through Suez`() {
        val lane = SeaLanes.lane(listOf("SINGAPORE", "ZEEBRUGGE"))
        assertTrue(lane.any { it.lat < -34 })
        assertTrue(lane.none { it.lat in 12.0..31.0 && it.lon in 32.0..44.0 })
    }

    @Test fun `the Mediterranean is reached through Gibraltar`() {
        val lane = SeaLanes.lane(listOf("SINGAPORE", "KOPER"))
        assertTrue(lane.any { it.lat in 35.5..36.5 && it.lon in -6.0..-5.0 })
        assertEquals(SeaLanes.ports.getValue("KOPER"), lane.last())
    }

    @Test fun `unknown ports are skipped`() {
        assertEquals(SeaLanes.lane(listOf("SHANGHAI", "ZEEBRUGGE")), SeaLanes.lane(listOf("SHANGHAI", "ATLANTIS", "zeebrugge")))
        assertEquals(listOf(SeaLanes.ports.getValue("SHANGHAI")), SeaLanes.lane(listOf("SHANGHAI", "ATLANTIS")))
    }

    @Test fun `longitudes past the antimeridian come back within range`() {
        assertEquals(-19.86, GeoPosition.of(16.97, -379.86).lon, 1e-9)
        assertEquals(34.80, GeoPosition.of(-31.65, -325.20).lon, 1e-9)
    }
}
