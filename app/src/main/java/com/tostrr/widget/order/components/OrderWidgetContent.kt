package com.tostrr.widget.order.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.tostrr.MainActivity
import com.tostrr.domain.order.Order
import com.tostrr.ui.common.formatDateTime

@Composable
internal fun OrderWidgetContent(order: Order?, car: Bitmap?) {
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
