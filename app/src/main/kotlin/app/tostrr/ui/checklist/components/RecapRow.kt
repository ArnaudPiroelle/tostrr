package app.tostrr.ui.checklist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.ui.theme.TostrrColors
import coil3.compose.AsyncImage
import java.io.File

/** One question of the recap; a tap opens it alone to change its answer. */
@Composable
internal fun RecapRow(text: String, answer: ChecklistAnswer?, onClick: () -> Unit, onPhoto: (String) -> Unit) {
    val (icon, tint, label) = when (answer?.status) {
        AnswerStatus.VERIFIED -> Triple(Icons.Filled.CheckCircle, TostrrColors.Positive, R.string.checklist_answer_verified)
        AnswerStatus.SKIPPED -> Triple(Icons.Filled.SkipNext, TostrrColors.Muted, R.string.checklist_answer_skipped)
        null -> Triple(Icons.Filled.RadioButtonUnchecked, TostrrColors.Muted, R.string.checklist_answer_none)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = stringResource(label), tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            answer?.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted, fontStyle = FontStyle.Italic)
            }
        }
        answer?.photoPath?.let { path ->
            Spacer(Modifier.width(10.dp))
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.common_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).clickable { onPhoto(path) },
            )
        }
    }
}
