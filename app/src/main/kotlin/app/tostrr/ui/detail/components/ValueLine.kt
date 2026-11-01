package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ValueLine(text: String, color: Color) {
    Text(
        text,
        Modifier.padding(top = 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
