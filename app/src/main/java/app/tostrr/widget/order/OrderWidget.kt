package app.tostrr.widget.order

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.height
import androidx.glance.state.PreferencesGlanceStateDefinition
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.widget.OrderRefKey
import app.tostrr.widget.order.components.OrderWidgetContent
import org.koin.core.annotation.Single

/** Home-screen version of the card shown in the orders list: the order picked for this widget and its step tracker. */
@Single
class OrderWidget(private val observeOrdersUseCase: ObserveOrdersUseCase) : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
                provideContent {
            // Read inside the composition: a Glance session outlives a single update, so the chosen order
            // and the orders list have to be observed rather than captured once.
            val chosen = currentState<Preferences>()[OrderRefKey]
            val orders by observeOrdersUseCase().collectAsState()
            // A widget with no choice yet, or whose order is gone, falls back to the first order.
            val order = orders.firstOrNull { it.reference == chosen } ?: orders.firstOrNull()
            val url = order?.imageUrl
            val car by produceState<Bitmap?>(null, url) { value = url?.let { loadBitmap(context, it) } }
            GlanceTheme { OrderWidgetContent(order, car) }
        }
    }

    /** Remote views can't hold hardware bitmaps and are size-limited, hence the explicit request. */
    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(480, 270).build()
        (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()?.trimTransparent()
    }.getOrNull()
}

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
