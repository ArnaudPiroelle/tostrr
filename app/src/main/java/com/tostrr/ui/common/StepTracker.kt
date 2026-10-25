package com.tostrr.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tostrr.domain.order.Order
import com.tostrr.ui.theme.TostrrColors

/** Size of a step dot and thickness of the bar behind it, shared by the list and detail trackers. */
val StepDotSize = 14.dp
val StepBarThickness = 3.dp

/**
 * One step marker: filled when done, ringed when current, hollow when pending.
 * Always opaque so the progress bar drawn behind never shows through.
 */
@Composable
fun StepDot(done: Boolean, current: Boolean, modifier: Modifier = Modifier, size: Dp = StepDotSize) {
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

/**
 * Horizontal tracker for the orders list. Same visual language as the vertical timeline in the detail
 * screen: a bar behind the dots, filled up to the current step.
 */
@Composable
fun StepTracker(order: Order, modifier: Modifier = Modifier) {
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
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        step.label,
                        style = StepLabelStyle,
                        fontWeight = if (step.done || i == current) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (step.done || i == current) TostrrColors.OnSurface else TostrrColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        step.date.orEmpty(),
                        modifier = Modifier.padding(top = 4.dp),
                        style = StepLabelStyle,
                        color = TostrrColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// Sans-serif keeps "immatriculé" / "Production" on one line at 6 columns on a phone.
private val StepLabelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = (-0.1).sp)
