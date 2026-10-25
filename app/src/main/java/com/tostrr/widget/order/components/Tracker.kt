package com.tostrr.widget.order.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tostrr.R
import com.tostrr.domain.order.Order

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

@Composable
private fun RowScope.Bar(visible: Boolean, color: ColorProvider, thickness: Dp) {
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
