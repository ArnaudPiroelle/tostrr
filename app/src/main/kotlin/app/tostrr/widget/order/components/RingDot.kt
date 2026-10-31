package app.tostrr.widget.order.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

/** A tinted disc with a smaller surface-coloured disc on top: Glance renders stroke-only shapes as filled. */
@Composable
internal fun RingDot(tint: ColorProvider, border: Dp, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Dot(tint, size)
        Dot(GlanceTheme.colors.surface, size - border * 2)
    }
}
