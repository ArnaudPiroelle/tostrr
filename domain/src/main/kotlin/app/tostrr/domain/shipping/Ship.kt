package app.tostrr.domain.shipping

import java.time.LocalDate
import java.time.LocalTime

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
 * digits of the six-digit serial ending a VIN. [done] once it has unloaded. [operator] is the shipping line,
 * [atBerth] whether it lies in port (at its [nextPort]) rather than sailing, [progress] how much of its route it has
 * sailed (0 to 1), [etaTime] the time of day of its [eta] when the site gives one, [draughtMeters] how deep it sits.
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
    val operator: String? = null,
    val atBerth: Boolean = false,
    val progress: Double? = null,
    val etaTime: LocalTime? = null,
    val draughtMeters: Double? = null,
)

/** One day's state of the tracked ships, as published at [takenAtEpochMs]. */
data class ShipSnapshot(val takenAtEpochMs: Long, val ships: List<Ship>)
