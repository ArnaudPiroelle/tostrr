package app.tostrr.widget.order.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.RowScope
import androidx.glance.layout.height
import androidx.glance.unit.ColorProvider

@Composable
internal fun RowScope.Bar(visible: Boolean, color: ColorProvider, thickness: Dp) {
    val base = GlanceModifier.defaultWeight().height(thickness)
    Box(if (visible) base.background(color) else base) {}
}
