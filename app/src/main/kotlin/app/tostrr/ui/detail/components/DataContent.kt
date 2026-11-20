package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R

/** What the export holds, and the button that writes it and opens the share sheet. */
@Composable
internal fun DataContent(onExport: () -> Unit) {
    Empty(stringResource(R.string.detail_data_export_text))
    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.detail_export))
    }
}
