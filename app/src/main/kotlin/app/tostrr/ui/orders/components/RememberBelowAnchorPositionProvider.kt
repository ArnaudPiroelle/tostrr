package app.tostrr.ui.orders.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

/** Places a tooltip under its anchor, right-aligned with it, keeping a margin to the screen edge. */
@Composable
internal fun rememberBelowAnchorPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density) {
        val gap = with(density) { 4.dp.roundToPx() }
        val edge = with(density) { 12.dp.roundToPx() }
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val maxX = (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge)
                val x = (anchorBounds.right - popupContentSize.width).coerceIn(edge, maxX)
                return IntOffset(x, anchorBounds.bottom + gap)
            }
        }
    }
}
