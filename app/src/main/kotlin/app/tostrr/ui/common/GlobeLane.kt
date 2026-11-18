package app.tostrr.ui.common

import app.tostrr.domain.shipping.Voyage

/**
 * A ship's sea lane as the globe draws it, split where the ship is: [sailed] ends on the ship, [ahead] starts from it.
 * Bending it through every port gets costly on a long route, so the screens' ViewModels build it off the main thread,
 * which makes it part of their public state: only the type is public, built and read within the app module.
 */
class GlobeLane internal constructor(internal val sailed: List<DoubleArray>, internal val ahead: List<DoubleArray>) {
    internal companion object {
        fun of(voyage: Voyage): GlobeLane {
            val path = laneThroughPorts(
                voyage.lane.map { GeoPoint(it.lat, it.lon) },
                voyage.ports.map { GeoPoint(it.position.lat, it.position.lon) },
            )
            val position = voyage.ship.position?.let { GeoPoint(it.lat, it.lon).toVector() } ?: return GlobeLane(path, emptyList())
            val nearest = path.indices.maxByOrNull { dot(path[it], position) } ?: return GlobeLane(emptyList(), emptyList())
            // The ship is rarely right on the hand-made lane: bent through it, the line has no kink there.
            val bent = bendThrough(path, nearest, position)
            return GlobeLane(bent.take(nearest + 1), bent.drop(nearest))
        }

        private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    }
}
