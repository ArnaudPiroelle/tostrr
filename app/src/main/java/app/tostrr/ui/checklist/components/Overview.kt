package app.tostrr.ui.checklist.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun ColumnScope.Overview(onStart: () -> Unit, onCancel: () -> Unit) {
    Header(tag = stringResource(R.string.checklist_tag_check), title = stringResource(R.string.checklist_title))
    Text(
        stringResource(R.string.checklist_intro),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = TostrrColors.Muted,
        textAlign = TextAlign.Center,
    )
    Card {
        DeliveryChecklist.sections.forEachIndexed { i, section ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = TostrrColors.Border)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                CountPill(stringResource(R.string.checklist_section_questions, section.questions.size))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = TostrrColors.Border)
        Text(
            stringResource(R.string.checklist_total, DeliveryChecklist.questionCount),
            Modifier.fillMaxWidth().padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
    PrimaryButton(stringResource(R.string.checklist_start), onClick = onStart)
    SecondaryButton(stringResource(R.string.common_cancel), onClick = onCancel)
}
