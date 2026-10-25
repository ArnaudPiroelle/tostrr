package app.tostrr.widget.countdown.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.tostrr.domain.order.Order
import app.tostrr.ui.MainActivity
import app.tostrr.widget.countdown.CountdownLabels

@Composable
internal fun CountdownWidgetContent(order: Order?, labels: CountdownLabels?) {
    val context = LocalContext.current
    val open = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            order?.let { data = Uri.parse("${MainActivity.WIDGET_SCHEME}://order/${it.reference}") }
        },
    )
    val root = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.surface)
        .cornerRadius(20.dp)
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .clickable(open)

    if (order == null || labels == null) {
        Row(root, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Ouvrez Tostrr",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
        }
        return
    }

    // Sized for a 2x1 (about 120 x 64dp) and grown when the widget is stretched; the big number's line is much
    // taller than its font size, hence the height budget.
    val size = LocalSize.current
    val k = minOf(size.width.value / 120f, size.height.value / 64f).coerceIn(0.7f, 2f)
    // Words like "Jour J" need a smaller font than a bare number to fit the same width.
    val valueSize = if (labels.value.length <= 3) 36 * k else 26 * k

    Row(root, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            labels.value,
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = valueSize.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        if (labels.unit.isNotEmpty()) {
            Text(
                labels.unit,
                modifier = GlanceModifier.padding(start = (6 * k).dp),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = (14 * k).sp),
                maxLines = 1,
            )
        }
    }
}
