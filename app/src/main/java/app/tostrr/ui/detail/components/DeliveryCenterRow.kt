package app.tostrr.ui.detail.components

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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.deliverycenter.googleMapsUrl
import app.tostrr.ui.theme.TostrrColors

/** The delivery centre; a tap opens it in Google Maps, at its exact address when the centre is known. */
@Composable
internal fun DeliveryCenterRow(name: String, center: DeliveryCenter?) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { runCatching { uriHandler.openUri(googleMapsUrl(name, center)) } }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(stringResource(R.string.detail_delivery_center), Modifier.weight(0.45f), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        Column(Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = TostrrColors.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.detail_delivery_center_maps), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Accent)
            }
        }
    }
}
