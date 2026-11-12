package app.tostrr.data.shipping

import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.shipping.GeoPosition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ShipTrackerTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()

    private val index = """
        {"snapshots":[
          {"id":"2026-11-10-2032","snapshotAt":"2026-11-10T19:32:56.786Z"},
          {"id":"2026-11-11-2111","snapshotAt":"2026-11-11T20:11:55.807Z"}
        ]}
    """

    private val snapshot = """
        {"id":"2026-11-11-2111","snapshotAt":"2026-11-11T20:11:55.807Z","ships":[
          {"name":"GLOVIS LIGHTHOUSE","line":"Hyundai Glovis","route":["NANGANG","SINGAPORE","MAURITIUS","ZEEBRUGGE"],
           "nextPort":"ZEEBRUGGE","eta":"2026-11-28","lat":-30.75,"lon":37.62,"heading":240,"vins":["051","052"],"done":false,
           "mode":"sail","prog":0.1942,"draught":10},
          {"name":"GLOVIS LEADER","route":["NANGANG","BARCELONA"],"nextPort":null,"eta":"2026-09-17 19:50",
           "lat":16.97,"lon":-379.86,"heading":null,"vins":[],"mode":"park","prog":1.2},
          {"line":"no name"}
        ]}
    """

    @Before fun clean() {
        File(context.filesDir, "ship_tracking.json").delete()
        http.on("/snapshots/index.json", index)
        http.on("/snapshots/2026-11-11-2111.json", snapshot)
    }

    @Test fun `reads the newest snapshot`() = runTest {
        val tracker = ShipTracker(http.client, context)
        tracker.refreshIfStale(now = 10_000_000_000L)

        val ships = tracker.snapshot.value!!.ships
        assertEquals(listOf("GLOVIS LIGHTHOUSE", "GLOVIS LEADER"), ships.map { it.name })
        val lighthouse = ships.first()
        assertEquals(LocalDate.of(2026, 11, 28), lighthouse.eta)
        assertEquals(240.0, lighthouse.headingDeg!!, 0.0)
        assertEquals(listOf("051", "052"), lighthouse.vinBlocks)
        assertFalse(lighthouse.done)
        assertEquals("Hyundai Glovis", lighthouse.operator)
        assertFalse(lighthouse.atBerth)
        assertEquals(0.1942, lighthouse.progress!!, 0.0)
        assertEquals(10.0, lighthouse.draughtMeters!!, 0.0)
        assertNull(lighthouse.etaTime)
        val leader = ships[1]
        assertEquals(LocalDate.of(2026, 9, 17), leader.eta)
        assertEquals(LocalTime.of(19, 50), leader.etaTime)
        assertTrue(leader.atBerth)
        assertEquals(1.0, leader.progress!!, 0.0)
        assertNull(leader.operator)
        assertEquals(GeoPosition.of(16.97, -19.86), leader.position)
        assertNull(leader.headingDeg)
        assertNull(leader.nextPort)
    }

    @Test fun `looks again only after three hours, and fetches a snapshot only when a new one is out`() = runTest {
        val tracker = ShipTracker(http.client, context)
        tracker.refreshIfStale(now = 10_000_000_000L)
        tracker.refreshIfStale(now = 10_000_000_000L + 60 * 60 * 1000L)
        assertEquals(1, http.callsTo("/snapshots/index.json").size)

        tracker.refreshIfStale(now = 10_000_000_000L + 4 * 60 * 60 * 1000L)
        assertEquals(2, http.callsTo("/snapshots/index.json").size)
        assertEquals(1, http.callsTo("/snapshots/2026-11-11-2111.json").size)
    }

    @Test fun `the last snapshot survives a restart and an outage, not a logout`() = runTest {
        ShipTracker(http.client, context).refreshIfStale(now = 10_000_000_000L)
        http.failOn("/snapshots/index.json")

        val restarted = ShipTracker(http.client, context)
        restarted.refreshIfStale(now = 20_000_000_000L)
        assertEquals(2, restarted.snapshot.value!!.ships.size)

        restarted.clear()
        assertNull(restarted.snapshot.value)
        assertNull(ShipTracker(http.client, context).snapshot.value)
        assertTrue(http.calls.none { it.request.url.host != "tff-ship-tracking.pages.dev" })
    }
}
