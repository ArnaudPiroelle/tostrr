package app.tostrr.ui.common

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * [path] (unit vectors, a densely sampled lane) bent so that its point [at] lands on [position], where the tracking
 * site puts the ship, off the hand-made lane: the points around it move towards the ship less and less with their
 * distance along the lane, none past [reachDeg] (or three times the gap, when wider, to keep the curve gentle), and
 * never past the lane's ends, which stay on their ports.
 */
internal fun bendThrough(path: List<DoubleArray>, at: Int, position: DoubleArray, reachDeg: Double = 6.0): List<DoubleArray> {
    if (at !in path.indices) return path
    val along = DoubleArray(path.size)
    for (i in 1 until path.size) along[i] = along[i - 1] + Math.toDegrees(arc(path[i - 1], path[i]))
    val reach = maxOf(reachDeg, 3 * Math.toDegrees(arc(path[at], position)))
    val reachBefore = minOf(reach, along[at])
    val reachAfter = minOf(reach, along.last() - along[at])
    val shift = DoubleArray(3) { position[it] - path[at][it] }
    return path.mapIndexed { i, point ->
        val distance = abs(along[i] - along[at])
        val sideReach = if (i < at) reachBefore else reachAfter
        val weight = when {
            i == at -> 1.0
            distance >= sideReach -> 0.0
            // 1 at the ship, easing to 0 at the reach, without a corner at either end.
            else -> 0.5 * (1 + cos(PI * distance / sideReach))
        }
        if (weight == 0.0) point else normalized(DoubleArray(3) { point[it] + shift[it] * weight })
    }
}

private fun arc(a: DoubleArray, b: DoubleArray) = acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0))

private fun normalized(v: DoubleArray): DoubleArray {
    val norm = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return DoubleArray(3) { v[it] / norm }
}

/**
 * The lane through [nodes] as a dense path that runs through every port on it, its corners at the sea waypoints
 * rounded. The sea net reaches some ports by a short detour from a waypoint, there and back (`W, port, W`), which drew
 * a spike beside the port: the detour is dropped and the lane bent from the waypoint through the port instead, within
 * [reachDeg].
 */
internal fun laneThroughPorts(nodes: List<GeoPoint>, ports: List<GeoPoint>, reachDeg: Double = 4.0): List<DoubleArray> {
    val kept = nodes.toMutableList()
    val detours = mutableListOf<Pair<Int, GeoPoint>>()
    var i = 1
    while (i < kept.lastIndex) {
        if (kept[i - 1] == kept[i + 1]) {
            detours += (i - 1) to kept[i]
            // The port and the waypoint's second visit; the earlier detours' indices, lower, stay right.
            repeat(2) { kept.removeAt(i) }
        } else {
            i++
        }
    }
    // Ports, and the waypoints about to be bent onto one, keep their corner.
    val sharp = detours.map { it.first }.toSet() + kept.indices.filter { kept[it] in ports }
    val (path, indices) = rounded(kept, sharp)
    return detours.fold(path) { lane, (node, port) -> bendThrough(lane, indices.getValue(node), port.toVector(), reachDeg) }
}

/**
 * The great-circle path through [nodes], a point every [stepDeg] at most, each corner but the [sharp] ones rounded
 * by a curve from a point on the way in to one on the way out, the corner as its control point. The curve stays
 * between the two legs, starting at most [maxCutDeg] before the corner and never past [CUT_SHARE] of either leg: where
 * waypoints are close, in a strait, the curve keeps tight. Returns the path, and the index in it of each sharp node.
 */
internal fun rounded(nodes: List<GeoPoint>, sharp: Set<Int>, maxCutDeg: Double = 3.0, stepDeg: Double = 0.5): Pair<List<DoubleArray>, Map<Int, Int>> {
    val v = nodes.map { it.toVector() }
    if (v.size < 2) return v to v.indices.associateWith { it }
    val legs = v.zipWithNext { a, b -> Math.toDegrees(arc(a, b)) }
    val cuts = DoubleArray(v.size) { i ->
        if (i == 0 || i == v.lastIndex || i in sharp) 0.0 else minOf(maxCutDeg, CUT_SHARE * legs[i - 1], CUT_SHARE * legs[i])
    }
    val path = mutableListOf(v[0])
    val indices = mutableMapOf(0 to 0)
    for (i in 1..v.lastIndex) {
        if (cuts[i] <= 0.0) {
            path.addArc(path.last(), v[i], stepDeg)
            indices[i] = path.lastIndex
        } else {
            val enter = towards(v[i], v[i - 1], cuts[i])
            val leave = towards(v[i], v[i + 1], cuts[i])
            path.addArc(path.last(), enter, stepDeg)
            val steps = maxOf(4, (2 * cuts[i] / stepDeg).toInt())
            for (s in 1..steps) {
                val t = s.toDouble() / steps
                path += normalized(DoubleArray(3) { (1 - t) * (1 - t) * enter[it] + 2 * (1 - t) * t * v[i][it] + t * t * leave[it] })
            }
        }
    }
    return path to indices
}

/** Adds the great-circle points after [from] up to [to], at most [stepDeg] apart. */
private fun MutableList<DoubleArray>.addArc(from: DoubleArray, to: DoubleArray, stepDeg: Double) {
    val angle = arc(from, to)
    val steps = maxOf(1, (Math.toDegrees(angle) / stepDeg).toInt())
    for (i in 1..steps) add(slerp(from, to, angle, i.toDouble() / steps))
}

/** The point [deg] degrees from [from] towards [to]. */
private fun towards(from: DoubleArray, to: DoubleArray, deg: Double): DoubleArray {
    val angle = arc(from, to)
    return slerp(from, to, angle, Math.toRadians(deg) / angle)
}

/** How much of a leg a corner's curve may take at most, so that two curves on the same leg never meet. */
private const val CUT_SHARE = 0.45
