package app.tostrr.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.shipping.Voyage
import app.tostrr.ui.common.portDisplayName
import app.tostrr.ui.common.shipDisplayName
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One ship that may carry the car, marked with [color] as on the globe when there are several; [onOpen] shows it in full. */
@Composable
internal fun ShipCandidate(voyage: Voyage, color: Color?, onOpen: () -> Unit) {
    val ship = voyage.ship
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (color != null) {
                Spacer(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Text(shipDisplayName(ship.name), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.size(4.dp))
        if (voyage.ports.size >= 2) {
            KeyValue(
                stringResource(R.string.detail_shipping_route),
                stringResource(
                    R.string.detail_shipping_route_value,
                    portDisplayName(voyage.ports.first().name),
                    portDisplayName(voyage.ports.last().name),
                ),
            )
        }
        KeyValue(stringResource(R.string.detail_shipping_next_port), ship.nextPort?.let(::portDisplayName))
        KeyValue(stringResource(R.string.detail_shipping_eta), ship.eta?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
        Spacer(Modifier.size(8.dp))
        FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.DirectionsBoat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.detail_shipping_open_ship))
        }
    }
}
