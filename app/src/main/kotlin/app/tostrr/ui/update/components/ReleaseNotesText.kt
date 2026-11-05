package app.tostrr.ui.update.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.tostrr.domain.update.ReleaseNotes

/** The release's Markdown [notes]. */
@Composable
internal fun ReleaseNotesText(notes: String) {
    val blocks = remember(notes) { ReleaseNotes.parse(notes) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            val text = annotated(block.spans)
            when (block) {
                is ReleaseNotes.Block.Heading -> Text(
                    text,
                    style = if (block.level <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is ReleaseNotes.Block.Paragraph -> Text(text, style = MaterialTheme.typography.bodyMedium)
                is ReleaseNotes.Block.Quote -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
                is ReleaseNotes.Block.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(block.marker, style = MaterialTheme.typography.bodyMedium)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun annotated(spans: List<ReleaseNotes.Span>): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        val style = SpanStyle(
            fontWeight = if (span.bold) FontWeight.Bold else null,
            fontFamily = if (span.code) FontFamily.Monospace else null,
        )
        withStyle(style) { append(span.text) }
    }
}
