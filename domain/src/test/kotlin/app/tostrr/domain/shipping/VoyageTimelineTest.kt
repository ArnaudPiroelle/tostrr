package app.tostrr.domain.shipping

import app.tostrr.domain.shipping.PortStatus.BERTHED
import app.tostrr.domain.shipping.PortStatus.DEPARTED
import app.tostrr.domain.shipping.PortStatus.LATER
import app.tostrr.domain.shipping.PortStatus.NEXT
import app.tostrr.domain.shipping.PortStatus.PASSED
import app.tostrr.domain.shipping.TimelineEntry.AtSea
import app.tostrr.domain.shipping.TimelineEntry.Port
import org.junit.Assert.assertEquals
import org.junit.Test

class VoyageTimelineTest {
    private fun ship(nextPort: String?, atBerth: Boolean = false) = Ship(
        name = "GLOVIS LIGHTHOUSE",
        route = listOf("NANGANG", "SINGAPORE", "MAURITIUS", "ZEEBRUGGE"),
        nextPort = nextPort,
        eta = null,
        position = null,
        headingDeg = null,
        vinBlocks = emptyList(),
        done = false,
        atBerth = atBerth,
        progress = 0.19,
    )

    @Test fun `a sailing ship is at sea before its next port`() {
        assertEquals(
            listOf(Port("NANGANG", DEPARTED), Port("SINGAPORE", PASSED), AtSea(0.19), Port("MAURITIUS", NEXT), Port("ZEEBRUGGE", LATER)),
            VoyageTimeline.of(ship("Mauritius")),
        )
    }

    @Test fun `a ship in port lies at its next port`() {
        assertEquals(
            listOf(Port("NANGANG", DEPARTED), Port("SINGAPORE", PASSED), Port("MAURITIUS", PASSED), Port("ZEEBRUGGE", BERTHED)),
            VoyageTimeline.of(ship("ZEEBRUGGE", atBerth = true)),
        )
    }

    @Test fun `without a known next port, only the first one is left`() {
        assertEquals(
            listOf(Port("NANGANG", DEPARTED), AtSea(0.19), Port("SINGAPORE", LATER), Port("MAURITIUS", LATER), Port("ZEEBRUGGE", LATER)),
            VoyageTimeline.of(ship(null)),
        )
    }
}
