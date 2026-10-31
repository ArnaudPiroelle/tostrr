package app.tostrr.ui.checklist.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.checklist.ChecklistSection
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun ColumnScope.SectionIntro(
    section: ChecklistSection,
    onStart: () -> Unit,
    onOpenQuestion: (questionId: String) -> Unit,
    onBack: (() -> Unit)?,
) {
    var expanded by rememberSaveable(section.id) { mutableStateOf(false) }
    Header(tag = stringResource(R.string.checklist_tag_check), title = stringResource(R.string.checklist_section_title, section.title))
    Card {
        Text(
            stringResource(R.string.checklist_section_ready, section.questions.size),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.checklist_start), trailing = Icons.Filled.KeyboardDoubleArrowRight, onClick = onStart)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TostrrColors.Border),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.checklist_section_see_questions), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TostrrColors.Muted,
                    )
                }
                if (expanded) {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        // A tap jumps straight to that question.
                        section.questions.forEachIndexed { i, question ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenQuestion(question.id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${i + 1}",
                                    Modifier.width(28.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TostrrColors.Accent,
                                )
                                Text(question.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TostrrColors.Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (onBack != null) SecondaryButton(stringResource(R.string.checklist_previous), leading = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onBack)
}
