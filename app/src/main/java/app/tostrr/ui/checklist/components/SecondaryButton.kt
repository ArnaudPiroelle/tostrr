package app.tostrr.ui.checklist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun SecondaryButton(
    text: String,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TostrrColors.Border),
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = TostrrColors.OnSurface)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailing, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
