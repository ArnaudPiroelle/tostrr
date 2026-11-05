package app.tostrr.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.theme.TostrrColors

/**
 * Horizontal tracker for the orders list. Same visual language as the vertical timeline in the detail
 * screen: a bar behind the dots, filled up to the current step.
 */
@Composable
internal fun StepTracker(order: Order, modifier: Modifier = Modifier) {
    val current = order.currentStepIndex
    val count = order.steps.size
    val track = TostrrColors.Track
    val accent = TostrrColors.Accent

    Column(modifier.fillMaxWidth()) {
        // One equal-width cell per step, so dot centres sit at (i + 0.5) * cellWidth.
        Box(
            Modifier
                .fillMaxWidth()
                .height(StepDotSize)
                .drawBehind {
                    val cell = size.width / count
                    val y = size.height / 2
                    val start = cell / 2
                    val stroke = StepBarThickness.toPx()
                    drawLine(track, Offset(start, y), Offset(start + cell * (count - 1), y), stroke, StrokeCap.Round)
                    if (current > 0) {
                        drawLine(accent, Offset(start, y), Offset(start + cell * current, y), stroke, StrokeCap.Round)
                    }
                },
        ) {
            Row(Modifier.fillMaxWidth()) {
                order.steps.forEachIndexed { i, step ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StepDot(done = step.done, current = i == current)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            order.steps.forEachIndexed { i, step ->
                Text(
                    stringResource(step.kind.labelRes),
                    modifier = Modifier.weight(1f),
                    style = StepLabelStyle,
                    fontWeight = if (step.done || i == current) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (step.done || i == current) TostrrColors.OnSurface else TostrrColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // A row of its own: the dates line up whether the labels above take one line or two.
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            order.steps.forEach { step ->
                Text(
                    step.date.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = StepLabelStyle,
                    color = TostrrColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// Sans-serif keeps "immatriculé" / "Production" on one line at 6 columns on a phone.
private val StepLabelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = (-0.1).sp)
