package app.tostrr.widget.order.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.tostrr.domain.order.Order

/**
 * Same look as the in-app tracker: a bar behind the dots, filled up to the current step. Glance can't
 * stack a bar under the dots, so each cell draws its own two half-bars on either side of its dot.
 */
@Composable
internal fun Tracker(order: Order, k: Float) {
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
