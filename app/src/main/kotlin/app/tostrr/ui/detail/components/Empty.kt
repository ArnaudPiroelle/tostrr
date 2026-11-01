package app.tostrr.ui.detail.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun Empty(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
