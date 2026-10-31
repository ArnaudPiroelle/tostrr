package app.tostrr.ui.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = TostrrColors.Accent, modifier = Modifier.padding(top = 2.dp).size(28.dp))
        Spacer(Modifier.width(20.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        }
    }
}
