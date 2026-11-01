package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.common.StepBarThickness
import app.tostrr.ui.common.StepDot
import app.tostrr.ui.common.StepDotSize
import app.tostrr.ui.common.labelRes
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun ProgressContent(order: Order) {
    val current = order.currentStepIndex
    val track = TostrrColors.Track
    val accent = TostrrColors.Accent
    val rowHeight = 44.dp
    val dotSize = StepDotSize
    val lastIndex = order.steps.lastIndex

    // The bar is drawn behind the rows, from the first dot to the last; the part up to the current step is filled.
    Column(
        Modifier.drawBehind {
            val x = dotSize.toPx() / 2
            val top = rowHeight.toPx() / 2
            val stroke = StepBarThickness.toPx()
            drawLine(track, Offset(x, top), Offset(x, top + rowHeight.toPx() * lastIndex), stroke, StrokeCap.Round)
            if (current > 0) {
                drawLine(accent, Offset(x, top), Offset(x, top + rowHeight.toPx() * current), stroke, StrokeCap.Round)
            }
        },
    ) {
        order.steps.forEachIndexed { i, step ->
            Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                StepDot(done = step.done, current = i == current)
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(step.kind.labelRes),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.done || i == current) TostrrColors.OnSurface else TostrrColors.Muted,
                )
                Text(
                    step.date.orEmpty().replace("\n", " – "),
                    style = MaterialTheme.typography.labelMedium,
                    color = TostrrColors.Muted,
                )
            }
        }
    }
}
