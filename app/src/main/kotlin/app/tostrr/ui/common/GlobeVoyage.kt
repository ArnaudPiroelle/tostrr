package app.tostrr.ui.common

import androidx.compose.ui.graphics.Color
import app.tostrr.domain.shipping.ShipVoyage
import app.tostrr.domain.shipping.Voyage
import kotlin.math.sqrt

/** A port of call on the globe; [passed] once the ship has left it. */
internal data class GlobePort(val name: String, val position: GeoPoint, val passed: Boolean)

/**
 * A ship on the globe, drawn in [color]: at [position] heading [headingDeg] when they are known, with its [ports] in
 * order and the sea [lane] it follows from the first port to the last.
 */
internal data class GlobeShip(
    val name: String,
    val position: GeoPoint?,
    val headingDeg: Double?,
    val ports: List<GlobePort>,
    val lane: List<GeoPoint>,
    val color: Color,
)

/** What the shipping globe shows: one ship, or every ship that may carry the car. */
internal data class GlobeVoyage(val ships: List<GlobeShip>) {
    /** Where the globe looks first, and back to on a double tap: between the ships, else the port they head for. */
    val focus: GeoPoint?
        get() {
            val points = ships.mapNotNull { it.position }.ifEmpty {
                ships.mapNotNull { ship -> (ship.ports.firstOrNull { !it.passed } ?: ship.ports.lastOrNull())?.position }
            }
            if (points.isEmpty()) return null
            val sum = DoubleArray(3)
            points.forEach { p -> p.toVector().forEachIndexed { i, v -> sum[i] += v } }
            val norm = sqrt(sum[0] * sum[0] + sum[1] * sum[1] + sum[2] * sum[2])
            return if (norm < 1e-6) points.first() else GeoPoint.ofVector(DoubleArray(3) { sum[it] / norm })
        }
}

/** The globe for [this] voyage, each candidate in its [candidateColor]. */
internal fun ShipVoyage.toGlobe(accent: Color) = GlobeVoyage(candidates.mapIndexed { index, it -> it.toGlobe(candidateColor(index, accent)) })

/** The ship of one voyage on the globe, drawn in [color]. */
internal fun Voyage.toGlobe(color: Color) = GlobeShip(
    name = shipDisplayName(ship.name),
    position = ship.position?.let { GeoPoint(it.lat, it.lon) },
    headingDeg = ship.headingDeg,
    ports = ports.map { GlobePort(portDisplayName(it.name), GeoPoint(it.position.lat, it.position.lon), it.passed) },
    lane = lane.map { GeoPoint(it.lat, it.lon) },
    color = color,
)
