package app.tostrr.ui.detail.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A 3D globe, night-styled, with the [voyage]'s ships, each in its colour with its sea lane (sailed solid, ahead dashed)
 * and its ports. One finger turns it, two zoom, a double tap brings the ships back to the centre.
 */
@Composable
internal fun ShipGlobe(voyage: GlobeVoyage, modifier: Modifier = Modifier, state: GlobeState = remember(voyage) { GlobeState(voyage.focus) }) {
    val context = LocalContext.current
    val land by produceState<FloatArray?>(null) { value = withContext(Dispatchers.IO) { GlobeLand.load(context) } }
    val focus = voyage.focus ?: GeoPoint(0.0, 0.0)
    val scope = rememberCoroutineScope()
    val pulse by rememberInfiniteTransition(label = "ship").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall.copy(shadow = Shadow(Color.Black, blurRadius = 6f))
    val shipStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    val lanes = remember(voyage) { voyage.ships.map(Lane::of) }
    // A port several ships call at is drawn once, as the first ship to list it has it.
    val ports = remember(voyage) { voyage.ships.flatMap { ship -> ship.ports.map { it to ship.color } }.distinctBy { it.first.name } }
    val dots = remember { LandDots() }
    val stars = remember { List(STARS) { Offset(Random(it).nextFloat(), Random(it + STARS).nextFloat()) } }

    Canvas(
        modifier
            .clipToBounds()
            .background(Space)
            .pointerInput(voyage, state) {
                detectTapGestures(onDoubleTap = {
                    scope.launch {
                        val fromLat = state.centerLat
                        val fromLon = state.centerLon
                        val toLat = focus.lat.coerceIn(-GlobeState.MAX_LAT, GlobeState.MAX_LAT)
                        val deltaLon = ((focus.lon - fromLon) % 360 + 540) % 360 - 180
                        val fromZoom = state.zoom
                        Animatable(0f).animateTo(1f, tween(700)) {
                            state.centerLat = fromLat + (toLat - fromLat) * value
                            state.centerLon = fromLon + deltaLon * value
                            state.zoom = fromZoom + (1f - fromZoom) * value
                        }
                    }
                })
            }
            .pointerInput(state) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    state.zoomBy(gestureZoom)
                    state.turn(pan.x, pan.y, globeRadius(size.width.toFloat(), size.height.toFloat(), state.zoom))
                }
            },
    ) {
        stars.forEach { drawCircle(Color.White.copy(alpha = 0.35f), 2.dp.toPx() / 2, Offset(it.x * size.width, it.y * size.height)) }
        val radius = globeRadius(size.width, size.height, state.zoom)
        val camera = GlobeCamera(state.centerLat, state.centerLon)
        drawSphere(radius)
        land?.let { dots.draw(this, it, camera, radius, state.zoom) }
        val several = voyage.ships.size > 1
        voyage.ships.zip(lanes).asReversed().forEach { (ship, lane) -> drawLane(lane, camera, radius, ship.color, several) }
        ports.forEach { (port, color) -> drawPort(port, camera, radius, color, textMeasurer, labelStyle) }
        val chips = mutableListOf<Rect>()
        voyage.ships.asReversed().forEach { drawShipMarker(it, camera, radius, pulse) }
        voyage.ships.forEach { drawShipLabel(it, camera, radius, textMeasurer, shipStyle, chips) }
    }
}

private fun globeRadius(width: Float, height: Float, zoom: Float) = min(width, height) / 2f * 0.86f * zoom

private fun DrawScope.toScreen(camera: GlobeCamera, radius: Float, v: DoubleArray): Offset? {
    if (camera.depth(v[0], v[1], v[2]) <= 0) return null
    return Offset(
        center.x + radius * camera.x(v[0], v[1], v[2]).toFloat(),
        center.y - radius * camera.y(v[0], v[1], v[2]).toFloat(),
    )
}

private fun DrawScope.drawSphere(radius: Float) {
    val glowRadius = radius * 1.18f
    drawCircle(
        Brush.radialGradient(0.8f to Atmosphere, 1f to Color.Transparent, center = center, radius = glowRadius),
        radius = glowRadius,
    )
    drawCircle(
        Brush.radialGradient(
            0f to OceanLit,
            0.55f to Ocean,
            1f to OceanDark,
            center = center + Offset(-0.35f * radius, -0.4f * radius),
            radius = radius * 1.5f,
        ),
        radius = radius,
    )
    drawCircle(Rim, radius = radius, style = Stroke(Stroke.HairlineWidth))
}

/** The land dots, in three bands of depth that fade towards the edge of the disc. */
private class LandDots {
    private var bands = Array(BANDS) { FloatArray(0) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    fun draw(scope: DrawScope, land: FloatArray, camera: GlobeCamera, radius: Float, zoom: Float) = with(scope) {
        if (bands[0].size < land.size) bands = Array(BANDS) { FloatArray(land.size / 3 * 2) }
        val counts = IntArray(BANDS)
        var i = 0
        while (i < land.size) {
            val x = land[i].toDouble()
            val y = land[i + 1].toDouble()
            val z = land[i + 2].toDouble()
            i += 3
            val depth = camera.depth(x, y, z)
            if (depth <= 0.02) continue
            val band = (depth * BANDS).toInt().coerceAtMost(BANDS - 1)
            val n = counts[band]
            bands[band][n] = center.x + radius * camera.x(x, y, z).toFloat()
            bands[band][n + 1] = center.y - radius * camera.y(x, y, z).toFloat()
            counts[band] = n + 2
        }
        paint.strokeWidth = 2.dp.toPx() * 0.8f * zoom.pow(0.6f)
        drawIntoCanvas { canvas ->
            for (band in 0 until BANDS) {
                paint.color = LandDot.copy(alpha = 0.3f + 0.6f * (band + 1) / BANDS).toArgb()
                canvas.nativeCanvas.drawPoints(bands[band], 0, counts[band], paint)
            }
        }
    }
}

/** The lane split where the ship is: [sailed] ends on the ship, [ahead] starts from it. */
private class Lane(val sailed: List<DoubleArray>, val ahead: List<DoubleArray>) {
    companion object {
        fun of(ship: GlobeShip): Lane {
            val path = greatCirclePath(ship.lane)
            val position = ship.position?.toVector() ?: return Lane(path, emptyList())
            val nearest = path.indices.maxByOrNull { dot(path[it], position) } ?: return Lane(emptyList(), emptyList())
            return Lane(path.take(nearest) + listOf(position), listOf(position) + path.drop(nearest + 1))
        }

        private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    }
}

/** The lane ahead is white for a lone ship, in each ship's colour when several may carry the car. */
private fun DrawScope.drawLane(lane: Lane, camera: GlobeCamera, radius: Float, color: Color, several: Boolean) {
    val sailed = visiblePath(lane.sailed, camera, radius)
    drawPath(sailed, color.copy(alpha = 0.25f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    drawPath(sailed, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    drawPath(
        visiblePath(lane.ahead, camera, radius),
        if (several) color.copy(alpha = 0.7f) else LaneAhead,
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))),
    )
}

/** The visible stretches of [points], broken where they go behind the globe. */
private fun DrawScope.visiblePath(points: List<DoubleArray>, camera: GlobeCamera, radius: Float) = Path().apply {
    var drawing = false
    points.forEach { v ->
        val point = toScreen(camera, radius, v)
        when {
            point == null -> drawing = false
            drawing -> lineTo(point.x, point.y)
            else -> moveTo(point.x, point.y).also { drawing = true }
        }
    }
}

private fun DrawScope.drawPort(
    port: GlobePort,
    camera: GlobeCamera,
    radius: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    style: androidx.compose.ui.text.TextStyle,
) {
    val v = port.position.toVector()
    if (camera.depth(v[0], v[1], v[2]) < 0.1) return
    val point = toScreen(camera, radius, v) ?: return
    if (port.passed) {
        drawCircle(color, 4.dp.toPx(), point)
    } else {
        drawCircle(Space, 4.dp.toPx(), point)
        drawCircle(Color.White, 4.dp.toPx(), point, style = Stroke(2.dp.toPx()))
    }
    val label = textMeasurer.measure(port.name, style.copy(color = Color.White.copy(alpha = 0.8f)))
    val gap = 6.dp.toPx()
    val x = if (point.x + gap + label.size.width <= size.width) point.x + gap else point.x - gap - label.size.width
    drawText(label, topLeft = Offset(x, point.y - label.size.height / 2f))
}

private fun DrawScope.drawShipMarker(ship: GlobeShip, camera: GlobeCamera, radius: Float, pulse: Float) {
    val position = ship.position ?: return
    val point = toScreen(camera, radius, position.toVector()) ?: return
    val color = ship.color
    drawCircle(color.copy(alpha = 0.5f * (1 - pulse)), 6.dp.toPx() + 14.dp.toPx() * pulse, point, style = Stroke(2.dp.toPx()))
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent), point, 14.dp.toPx()), 14.dp.toPx(), point)

    ship.headingDeg?.let { toScreen(camera, radius, position.moved(it, 2.0).toVector()) }?.let { ahead ->
        val direction = ahead - point
        val length = sqrt(direction.x * direction.x + direction.y * direction.y)
        if (length > 0.5f) {
            val unit = direction / length
            val side = Offset(-unit.y, unit.x)
            val tip = point + unit * 14.dp.toPx()
            val base = point + unit * 8.dp.toPx()
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    (base + side * 4.dp.toPx()).let { lineTo(it.x, it.y) }
                    (base - side * 4.dp.toPx()).let { lineTo(it.x, it.y) }
                    close()
                },
                color,
            )
        }
    }
    drawCircle(color, 6.dp.toPx(), point)
    drawCircle(Color.White, 2.dp.toPx(), point)
}

/**
 * The ship's name in a chip centred under it, where it hides neither the lane already sailed nor the ports' labels
 * beside it; pushed further down while it would cover a chip already in [placed].
 */
private fun DrawScope.drawShipLabel(
    ship: GlobeShip,
    camera: GlobeCamera,
    radius: Float,
    textMeasurer: TextMeasurer,
    style: androidx.compose.ui.text.TextStyle,
    placed: MutableList<Rect>,
) {
    val point = ship.position?.let { toScreen(camera, radius, it.toVector()) } ?: return
    val label = textMeasurer.measure(ship.name, style.copy(color = Color.White))
    val padding = Offset(8.dp.toPx(), 4.dp.toPx())
    val chip = Size(label.size.width + padding.x * 2, label.size.height + padding.y * 2)
    val margin = 8.dp.toPx()
    var rect = Rect(
        Offset((point.x - chip.width / 2).coerceIn(margin, (size.width - chip.width - margin).coerceAtLeast(margin)), point.y + 20.dp.toPx()),
        chip,
    )
    while (placed.any { it.overlaps(rect) }) rect = rect.translate(0f, chip.height + 4.dp.toPx())
    placed += rect
    drawRoundRect(ChipBackground, rect.topLeft, chip, CornerRadius(chip.height / 2))
    drawRoundRect(ship.color.copy(alpha = 0.8f), rect.topLeft, chip, CornerRadius(chip.height / 2), style = Stroke(2.dp.toPx() / 2))
    drawText(label, topLeft = rect.topLeft + padding)
}

private const val BANDS = 3
private const val STARS = 70

private val Space = Color(0xFF03060F)
private val Atmosphere = Color(0x664FA3FF)
private val OceanLit = Color(0xFF21457A)
private val Ocean = Color(0xFF0E2348)
private val OceanDark = Color(0xFF060E22)
private val Rim = Color(0x663D7BD9)
private val LandDot = Color(0xFFB9D4FF)
private val LaneAhead = Color(0xB3FFFFFF)
private val ChipBackground = Color(0xD90B1530)
