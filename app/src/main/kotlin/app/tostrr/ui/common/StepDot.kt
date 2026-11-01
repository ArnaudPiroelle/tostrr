package app.tostrr.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

/**
 * One step marker: filled when done, ringed when current, hollow when pending.
 * Always opaque so the progress bar drawn behind never shows through.
 */
@Composable
internal fun StepDot(done: Boolean, current: Boolean, modifier: Modifier = Modifier, size: Dp = StepDotSize) {
    val accent = TostrrColors.Accent
    val track = TostrrColors.Track
    val card = TostrrColors.Surface
    val base = modifier.size(size).clip(CircleShape)
    Box(
        when {
            done -> base.background(accent)
            current -> base.background(card).border(3.dp, accent, CircleShape)
            else -> base.background(card).border(2.dp, track, CircleShape)
        },
    )
}
