package app.tostrr.ui.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.ui.theme.TostrrColors

/**
 * Entry to the delivery checklist. Highlighted on the day of the appointment, when it is what you want open while
 * inspecting the car; otherwise a quieter card, so it can be prepared or reviewed any day.
 */
@Composable
internal fun ChecklistCard(deliveryDay: Boolean, checklist: Checklist?, onOpen: () -> Unit) {
    val answered = checklist?.answers?.size ?: 0
    val (subtitle, action) = when {
        checklist == null -> stringResource(R.string.detail_checklist_not_started) to stringResource(R.string.detail_checklist_start)
        checklist.completed -> stringResource(R.string.detail_checklist_completed, checklist.verifiedPercent) to stringResource(R.string.detail_checklist_recap)
        else -> stringResource(R.string.detail_checklist_in_progress, answered, DeliveryChecklist.questionCount) to stringResource(R.string.detail_checklist_resume)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (deliveryDay) TostrrColors.Accent.copy(alpha = 0.12f) else TostrrColors.Surface,
        border = if (deliveryDay) null else BorderStroke(0.5.dp, TostrrColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = TostrrColors.Accent, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column {
                    Text(
                        stringResource(if (deliveryDay) R.string.detail_checklist_delivery_day else R.string.detail_block_checklist),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
                }
            }
            if (deliveryDay || checklist != null) {
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(action)
                }
            } else {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(action)
                }
            }
        }
    }
}
