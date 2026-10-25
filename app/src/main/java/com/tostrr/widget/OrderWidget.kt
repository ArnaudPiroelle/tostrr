package com.tostrr.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.tostrr.MainActivity
import com.tostrr.R
import com.tostrr.data.OrderRepository
import com.tostrr.domain.Order
import com.tostrr.ui.common.formatDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Which order a widget instance shows; stored in its Glance state. */
val OrderRefKey = stringPreferencesKey("order_reference")

/** Home-screen version of the card shown in the orders list: the order picked for this widget and its step tracker. */
class OrderWidget : GlanceAppWidget(), KoinComponent {
    private val repository: OrderRepository by inject()

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
                provideContent {
            // Read inside the composition: a Glance session outlives a single update, so the chosen order
            // and the orders list have to be observed rather than captured once.
            val chosen = currentState<Preferences>()[OrderRefKey]
            val orders by repository.orders.collectAsState()
            // A widget with no choice yet, or whose order is gone, falls back to the first order.
            val order = orders.firstOrNull { it.reference == chosen } ?: orders.firstOrNull()
            val url = order?.imageUrl
            val car by produceState<Bitmap?>(null, url) { value = url?.let { loadBitmap(context, it) } }
            GlanceTheme { WidgetContent(order, car) }
        }
    }

    /** Remote views can't hold hardware bitmaps and are size-limited, hence the explicit request. */
    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(480, 270).build()
        (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()?.trimTransparent()
    }.getOrNull()
}

@Composable
private fun WidgetContent(order: Order?, car: Bitmap?) {
    val context = LocalContext.current
    // A distinct data URI per order keeps each widget's PendingIntent separate (extras alone wouldn't).
    val open = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            order?.let { data = Uri.parse("${MainActivity.WIDGET_SCHEME}://order/${it.reference}") }
        },
    )
    val root = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.surface)
        .cornerRadius(20.dp)
        .padding(12.dp)
        .clickable(open)

    if (order == null) {
        Box(root, contentAlignment = Alignment.Center) {
            Text("Aucune commande, ouvrez Tostrr", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        }
        return
    }

    // Everything grows with the widget so a larger one isn't just a small card with empty space around it.
    val size = LocalSize.current
    val k = minOf(size.width.value / 300f, size.height.value / 150f).coerceIn(0.85f, 1.5f)

    Column(root) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Commande ", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (9 * k).sp))
                    Text(
                        "#${order.reference}",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = (11 * k).sp, fontWeight = FontWeight.Bold),
                    )
                }
                Text(order.modelName, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = (13 * k).sp))
                Text(
                    "VIN: ${order.vin ?: "inconnu"}",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (10 * k).sp),
                )
                order.deliverySlot?.let {
                    Text("Créneau : $it", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (10 * k).sp))
                }
            }
            if (car != null) {
                Image(
                    provider = ImageProvider(car),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(width = (124 * k).dp, height = (64 * k).dp),
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Tracker(order, k)
        Spacer(GlanceModifier.defaultWeight())
        Text(
            "Mise à jour ${formatDateTime(order.fetchedAtEpochMs)}",
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (9 * k).sp, textAlign = TextAlign.End),
        )
    }
}

/**
 * Same look as the in-app tracker: a bar behind the dots, filled up to the current step. Glance can't
 * stack a bar under the dots, so each cell draws its own two half-bars on either side of its dot.
 */
@Composable
private fun Tracker(order: Order, k: Float) {
    val current = order.currentStepIndex
    val last = order.steps.lastIndex
    val accent = GlanceTheme.colors.primary
    val track = GlanceTheme.colors.outline

    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        order.steps.forEachIndexed { i, step ->
            Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Bar(visible = i > 0, color = if (i <= current) accent else track, thickness = 3.dp * k)
                when {
                    step.done -> Dot(accent, DotSize * k)
                    i == current -> RingDot(accent, border = 3.dp * k, size = DotSize * k)
                    else -> RingDot(track, border = 2.dp * k, size = DotSize * k)
                }
                Bar(visible = i < last, color = if (i < current) accent else track, thickness = 3.dp * k)
            }
        }
    }
    Row(GlanceModifier.fillMaxWidth().padding(top = (3 * k).dp)) {
        order.steps.forEachIndexed { i, step ->
            val strong = step.done || i == current
            Text(
                step.label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = if (strong) GlanceTheme.colors.onSurface else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = (8 * k).sp,
                    fontWeight = if (strong) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.Bar(visible: Boolean, color: ColorProvider, thickness: Dp) {
    val base = GlanceModifier.defaultWeight().height(thickness)
    Box(if (visible) base.background(color) else base) {}
}

@Composable
private fun Dot(tint: ColorProvider, size: Dp = DotSize) {
    Image(
        provider = ImageProvider(R.drawable.widget_dot_fill),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(size),
    )
}

/** A tinted disc with a smaller surface-coloured disc on top: Glance renders stroke-only shapes as filled. */
@Composable
private fun RingDot(tint: ColorProvider, border: Dp, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Dot(tint, size)
        Dot(GlanceTheme.colors.surface, size - border * 2)
    }
}

private val DotSize = 14.dp

/** The configurator renders the car with wide transparent margins; cropping lets it fill the widget. */
private fun Bitmap.trimTransparent(): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width; var right = -1; var top = height; var bottom = -1
    for (y in 0 until height) for (x in 0 until width) {
        if ((pixels[y * width + x] ushr 24) > 16) {
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }
    if (right < left || bottom < top) return this
    return Bitmap.createBitmap(this, left, top, right - left + 1, bottom - top + 1)
}
