package app.tostrr.ui.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.theme.TostrrColors

/** Opens the car's parts on parts.tesla.com. */
@Composable
internal fun PartsCatalogRow(url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { runCatching { uriHandler.openUri(url) } }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.detail_vehicle_parts_catalog), Modifier.weight(0.45f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        Row(Modifier.weight(0.55f), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TostrrColors.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.detail_vehicle_parts_catalog_open), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Accent)
        }
    }
}
