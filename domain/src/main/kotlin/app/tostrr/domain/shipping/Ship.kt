package app.tostrr.domain.shipping

import java.time.LocalDate

/** A point on Earth, in degrees, the longitude within -180..180. */
data class GeoPosition(val lat: Double, val lon: Double) {
    companion object {
        /** [lon] brought back within -180..180: the tracking site lets it run past them. */
        fun of(lat: Double, lon: Double) = GeoPosition(lat, ((lon + 180) % 360 + 360) % 360 - 180)
    }
}

/**
 * A car carrier as the community ship tracking site follows it: its [route] (port names, upper case), the port it
 * heads for, its last known [position] and the VIN blocks it is believed to carry: [vinBlocks] are the first three
 * digits of the six-digit serial ending a VIN. [done] once it has unloaded.
 */
data class Ship(
    val name: String,
    val route: List<String>,
    val nextPort: String?,
    val eta: LocalDate?,
    val position: GeoPosition?,
    val headingDeg: Double?,
    val vinBlocks: List<String>,
    val done: Boolean,
)

/** One day's state of the tracked ships, as published at [takenAtEpochMs]. */
data class ShipSnapshot(val takenAtEpochMs: Long, val ships: List<Ship>)
