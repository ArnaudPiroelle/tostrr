package app.tostrr.ui.common

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth, in degrees. */
internal data class GeoPoint(val lat: Double, val lon: Double) {
    /** Its unit vector, x towards (0°, 0°), z towards the North Pole. */
    fun toVector(): DoubleArray {
        val lat = Math.toRadians(lat)
        val lon = Math.toRadians(lon)
        return doubleArrayOf(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
    }

    /** The point [distanceDeg] degrees of arc away, towards [bearingDeg] (0 = north, clockwise). */
    fun moved(bearingDeg: Double, distanceDeg: Double): GeoPoint {
        val lat1 = Math.toRadians(lat)
        val bearing = Math.toRadians(bearingDeg)
        val distance = Math.toRadians(distanceDeg)
        val lat2 = asin(sin(lat1) * cos(distance) + cos(lat1) * sin(distance) * cos(bearing))
        val lon2 = Math.toRadians(lon) +
            atan2(sin(bearing) * sin(distance) * cos(lat1), cos(distance) - sin(lat1) * sin(lat2))
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    companion object {
        fun ofVector(v: DoubleArray) = GeoPoint(Math.toDegrees(asin(v[2].coerceIn(-1.0, 1.0))), Math.toDegrees(atan2(v[1], v[0])))
    }
}

/**
 * An orthographic view of the globe centred on ([lat], [lon]): [project] gives a unit vector's position on the disc,
 * x to the east and y to the north in units of the radius, and its depth, positive on the visible side.
 */
internal class GlobeCamera(lat: Double, lon: Double) {
    private val east: DoubleArray
    private val north: DoubleArray
    private val out: DoubleArray

    init {
        val phi = Math.toRadians(lat)
        val lambda = Math.toRadians(lon)
        east = doubleArrayOf(-sin(lambda), cos(lambda), 0.0)
        north = doubleArrayOf(-sin(phi) * cos(lambda), -sin(phi) * sin(lambda), cos(phi))
        out = doubleArrayOf(cos(phi) * cos(lambda), cos(phi) * sin(lambda), sin(phi))
    }

    fun x(x: Double, y: Double, z: Double) = x * east[0] + y * east[1] + z * east[2]
    fun y(x: Double, y: Double, z: Double) = x * north[0] + y * north[1] + z * north[2]
    fun depth(x: Double, y: Double, z: Double) = x * out[0] + y * out[1] + z * out[2]
}

/** The great-circle path through [points], one vertex at most every [stepDeg] degrees of arc, as unit vectors. */
internal fun greatCirclePath(points: List<GeoPoint>, stepDeg: Double = 1.0): List<DoubleArray> {
    val vectors = points.map { it.toVector() }
    if (vectors.size < 2) return vectors
    val path = mutableListOf(vectors.first())
    vectors.zipWithNext { a, b ->
        val angle = acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0))
        val steps = maxOf(1, (Math.toDegrees(angle) / stepDeg).toInt())
        for (i in 1..steps) path += slerp(a, b, angle, i.toDouble() / steps)
    }
    return path
}

private fun slerp(a: DoubleArray, b: DoubleArray, angle: Double, t: Double): DoubleArray {
    if (angle < 1e-9) return b
    val wa = sin((1 - t) * angle) / sin(angle)
    val wb = sin(t * angle) / sin(angle)
    val v = DoubleArray(3) { wa * a[it] + wb * b[it] }
    val norm = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return DoubleArray(3) { v[it] / norm }
}
