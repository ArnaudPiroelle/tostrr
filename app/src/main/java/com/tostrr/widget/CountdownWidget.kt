package com.tostrr.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.tostrr.MainActivity
import com.tostrr.TostrrApp
import com.tostrr.domain.DeliveryCountdown
import com.tostrr.domain.Order
import com.tostrr.domain.deliveryCountdown

/** 2x1 countdown to the delivery: days to the appointment, or to / inside the delivery window while there is none. */
class CountdownWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as TostrrApp).repository
        provideContent {
            val chosen = currentState<Preferences>()[OrderRefKey]
            val orders by repository.orders.collectAsState()
            val order = orders.firstOrNull { it.reference == chosen } ?: orders.firstOrNull()
            // Today is read on every composition: each refresh's updateAll moves the count past midnight.
            GlanceTheme { CountdownContent(order, order?.deliveryCountdown()?.let(::countdownLabels)) }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

/**
 * What the widget shows: the big value and the unit next to it. Without room for a lead-in, the value tells the
 * states apart: a bare count before the window or the appointment, "+N" once inside the window.
 */
internal data class CountdownLabels(val value: String, val unit: String)

internal fun countdownLabels(countdown: DeliveryCountdown): CountdownLabels = when (countdown) {
    is DeliveryCountdown.Appointment -> when {
        countdown.days > 0 -> CountdownLabels("${countdown.days}", days(countdown.days))
        countdown.days == 0L -> CountdownLabels("Jour J", "")
        else -> CountdownLabels("J+${-countdown.days}", "")
    }
    is DeliveryCountdown.BeforeWindow -> CountdownLabels("${countdown.days}", days(countdown.days))
    is DeliveryCountdown.InWindow ->
        if (countdown.days == 0L) CountdownLabels("Jour 1", "")
        else CountdownLabels("+${countdown.days}", days(countdown.days))
    is DeliveryCountdown.PastWindow -> CountdownLabels("+${countdown.days}", "j de retard")
    DeliveryCountdown.Unknown -> CountdownLabels("—", "")
}

private fun days(n: Long) = if (n > 1) "jours" else "jour"

@Composable
private fun CountdownContent(order: Order?, labels: CountdownLabels?) {
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
