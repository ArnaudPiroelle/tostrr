package app.tostrr.widget.order.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import app.tostrr.R

@Composable
internal fun Dot(tint: ColorProvider, size: Dp = DotSize) {
    Image(
        provider = ImageProvider(R.drawable.widget_dot_fill),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(size),
    )
}
