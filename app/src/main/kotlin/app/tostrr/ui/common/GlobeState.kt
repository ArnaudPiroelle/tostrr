package app.tostrr.ui.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** Where the globe looks and how close: its centre in degrees and its [zoom], 1 for the whole disc in view. */
@Stable
internal class GlobeState(focus: GeoPoint?) {
    var centerLat by mutableDoubleStateOf((focus?.lat ?: 0.0).coerceIn(-MAX_LAT, MAX_LAT))
    var centerLon by mutableDoubleStateOf(focus?.lon ?: 0.0)
    var zoom by mutableFloatStateOf(1f)

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /** Turns the globe as if dragged by [dx], [dy] pixels on a disc of [radius] pixels. */
    fun turn(dx: Float, dy: Float, radius: Float) {
        centerLon -= Math.toDegrees(dx / radius.toDouble())
        centerLat = (centerLat + Math.toDegrees(dy / radius.toDouble())).coerceIn(-MAX_LAT, MAX_LAT)
    }

    companion object {
        const val MAX_LAT = 80.0
        const val MIN_ZOOM = 0.8f
        const val MAX_ZOOM = 5f
    }
}
