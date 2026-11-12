package app.tostrr.ui.ship.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

/** One line of the timeline: its dot on the line joining it to the others ([lineAbove], [lineBelow]), its text. */
@Composable
internal fun ShipTimelineRow(
    title: String,
    trailing: String?,
    dot: TimelineDot,
    color: Color,
    lineAbove: Color?,
    lineBelow: Color?,
) {
    val track = TostrrColors.Track
    val surface = TostrrColors.Surface
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(24.dp).fillMaxHeight()) {
            val middle = Offset(size.width / 2, size.height / 2)
            val width = 2.dp.toPx()
            lineAbove?.let { drawLine(it, Offset(middle.x, 0f), middle, width) }
            lineBelow?.let { drawLine(it, middle, Offset(middle.x, size.height), width) }
            when (dot) {
                TimelineDot.LEFT -> drawCircle(color, 6.dp.toPx(), middle)
                TimelineDot.CURRENT -> {
                    drawCircle(surface, 10.dp.toPx(), middle)
                    drawCircle(color, 9.dp.toPx(), middle, style = Stroke(width))
                    drawCircle(color, 4.dp.toPx(), middle)
                }
                TimelineDot.AHEAD -> {
                    drawCircle(surface, 6.dp.toPx(), middle)
                    drawCircle(track, 5.dp.toPx(), middle, style = Stroke(width))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            Modifier.weight(1f).padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (dot == TimelineDot.CURRENT) FontWeight.SemiBold else FontWeight.Normal,
        )
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (dot == TimelineDot.CURRENT) FontWeight.SemiBold else FontWeight.Normal,
                color = TostrrColors.Muted,
            )
        }
    }
}
