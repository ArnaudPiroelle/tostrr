package app.tostrr.ui.detail.components

import android.content.ClipData
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.theme.TostrrColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COPIED_FEEDBACK_MS = 2_000L

@Composable
internal fun VinRow(vin: String?, modifier: Modifier = Modifier) {
    // Fixed height so the header doesn't move when the VIN arrives.
    Row(modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.order_vin, vin ?: stringResource(R.string.order_vin_unknown)),
            style = MaterialTheme.typography.labelMedium,
            color = TostrrColors.Muted,
        )
        if (vin != null) {
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            val label = stringResource(R.string.detail_vin_clip_label)
            var copied by remember { mutableStateOf(false) }
            LaunchedEffect(copied) {
                if (copied) {
                    delay(COPIED_FEEDBACK_MS)
                    copied = false
                }
            }
            IconButton(
                onClick = {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, vin))) }
                    copied = true
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(if (copied) R.string.detail_vin_copied else R.string.detail_vin_copy),
                    modifier = Modifier.size(16.dp),
                    tint = if (copied) TostrrColors.Accent else TostrrColors.Muted,
                )
            }
        }
    }
}
