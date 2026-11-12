package app.tostrr.ui.common

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.acos
import kotlin.math.sqrt

class LaneBendTest {
    /** Along the equator from 0° to 30° east, a point about every degree. */
    private val path = greatCirclePath(listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 30.0)))
    private val ship = GeoPoint(-1.0, 15.0).toVector()

    private fun degrees(a: DoubleArray, b: DoubleArray) =
        Math.toDegrees(acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0)))

    @Test fun `the lane passes through the ship`() {
        val bent = bendThrough(path, 15, ship)

        assertArrayEquals(ship, bent[15], 1e-9)
    }

    @Test fun `it bends gradually, leaving the lane as it was past the reach`() {
        val bent = bendThrough(path, 15, ship)

        // No step between neighbours much longer than the lane's own one-degree steps.
        bent.zipWithNext().forEach { (a, b) -> assertTrue(degrees(a, b) < 1.2) }
        listOf(0, 8, 22, path.lastIndex).forEach { assertArrayEquals(path[it], bent[it], 1e-12) }
        assertTrue(degrees(path[13], bent[13]) in 0.1..0.99)
    }

    @Test fun `the ends stay on their ports when the ship is near one`() {
        val bent = bendThrough(path, 2, GeoPoint(-1.0, 2.0).toVector())

        assertArrayEquals(path[0], bent[0], 1e-12)
        assertArrayEquals(path.last(), bent.last(), 1e-12)
    }

    @Test fun `a port reached by a detour is on the lane, without the spike`() {
        val waypoint = GeoPoint(-20.0, 60.0)
        val mauritius = GeoPoint(-20.16, 57.5)
        val lane = listOf(GeoPoint(-5.0, 80.0), waypoint, mauritius, waypoint, GeoPoint(-27.0, 46.0))

        val path = laneThroughPorts(lane, listOf(lane.first(), mauritius, lane.last()))

        assertTrue(path.any { degrees(it, mauritius.toVector()) < 1e-9 })
        // Stretched and squeezed along the way around the port, but never a jump.
        path.zipWithNext().forEach { (a, b) -> assertTrue(degrees(a, b) < 1.6) }
        assertTrue(path.none { degrees(it, waypoint.toVector()) < 1e-9 })
        assertArrayEquals(lane.first().toVector(), path.first(), 1e-12)
        assertArrayEquals(lane.last().toVector(), path.last(), 1e-12)
    }

    @Test fun `corners at waypoints are rounded, ports stay put`() {
        val start = GeoPoint(0.0, 0.0)
        val corner = GeoPoint(0.0, 20.0)
        val end = GeoPoint(20.0, 20.0)

        val path = laneThroughPorts(listOf(start, corner, end), listOf(start, end))

        assertArrayEquals(start.toVector(), path.first(), 1e-12)
        assertArrayEquals(end.toVector(), path.last(), 1e-12)
        val cut = path.minOf { degrees(it, corner.toVector()) }
        assertTrue("cut $cut", cut in 0.2..2.5)
        assertTrue("turn ${maxTurn(path)}", maxTurn(path) < 20.0)
    }

    /** The sharpest change of direction between two successive steps, in degrees. */
    private fun maxTurn(path: List<DoubleArray>): Double = path.windowed(3).maxOf { (a, b, c) ->
        val u = DoubleArray(3) { b[it] - a[it] }
        val v = DoubleArray(3) { c[it] - b[it] }
        val cos = (u[0] * v[0] + u[1] * v[1] + u[2] * v[2]) / (sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]) * sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]))
        Math.toDegrees(acos(cos.coerceIn(-1.0, 1.0)))
    }
}
