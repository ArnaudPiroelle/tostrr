package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun GroupTitle(text: String) {
    Text(
        text.uppercase(),
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = TostrrColors.Muted,
    )
}
