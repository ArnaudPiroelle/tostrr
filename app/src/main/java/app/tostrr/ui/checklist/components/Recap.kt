package app.tostrr.ui.checklist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.tostrr.R
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors
import coil3.compose.AsyncImage
import java.io.File

@Composable
internal fun ColumnScope.Recap(
    checklist: Checklist,
    onEdit: () -> Unit,
    onEditQuestion: (questionId: String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var zoomedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val accent = TostrrColors.Accent
    val skipped = checklist.answers.values.count { it.status == AnswerStatus.SKIPPED }

    Header(tag = stringResource(R.string.checklist_recap_tag), title = stringResource(R.string.checklist_recap_title))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.checklist_recap_check), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp)
            Text(stringResource(R.string.checklist_percent, checklist.verifiedPercent), fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(
                stringResource(R.string.checklist_progress, checklist.verifiedCount, DeliveryChecklist.questionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = TostrrColors.Muted,
            )
        }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = TostrrColors.Positive.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = TostrrColors.Positive, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.checklist_recap_done), style = MaterialTheme.typography.titleMedium)
                checklist.completedAtEpochMs?.let {
                    Text(formatDateTime(it), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.checklist_recap_counts, checklist.verifiedCount, skipped),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
        )
    }
    DeliveryChecklist.sections.forEach { section ->
        val verified = section.questions.count { checklist.answers[it.id]?.status == AnswerStatus.VERIFIED }
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CountPill(stringResource(R.string.checklist_progress, verified, section.questions.size))
            }
            section.questions.forEach { question ->
                RecapRow(
                    question.text,
                    checklist.answers[question.id],
                    onClick = { onEditQuestion(question.id) },
                    onPhoto = { zoomedPhoto = it },
                )
            }
        }
    }
    PrimaryButton(stringResource(R.string.checklist_recap_edit), onClick = onEdit)
    SecondaryButton(stringResource(R.string.checklist_recap_delete), leading = Icons.Filled.Delete, onClick = onDelete)
    SecondaryButton(stringResource(R.string.common_close), onClick = onClose)

    zoomedPhoto?.let { path ->
        Dialog(onDismissRequest = { zoomedPhoto = null }) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.common_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { zoomedPhoto = null },
            )
        }
    }
}
