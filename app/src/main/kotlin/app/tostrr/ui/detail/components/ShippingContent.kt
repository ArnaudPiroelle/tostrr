package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.shipping.ShipVoyage
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun ShippingContent(voyage: ShipVoyage) {
    val accent = TostrrColors.Accent
    val globe = remember(voyage, accent) { voyage.toGlobe(accent) }
    Column {
        ShipGlobe(globe, Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.detail_shipping_hint), style = MaterialTheme.typography.bodySmall, color = TostrrColors.Muted)
        Spacer(Modifier.height(12.dp))
        if (voyage.uncertain) {
            Surface(shape = RoundedCornerShape(12.dp), color = TostrrColors.SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = TostrrColors.Muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.detail_shipping_candidates, voyage.candidates.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        voyage.candidates.forEachIndexed { index, candidate ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = TostrrColors.Border)
            ShipCandidate(candidate, globe.ships[index].color.takeIf { voyage.uncertain })
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.detail_shipping_source, formatDateTime(voyage.snapshotAtEpochMs)),
            style = MaterialTheme.typography.bodySmall,
            color = TostrrColors.Muted,
        )
        ShippingSourceRow()
    }
}
