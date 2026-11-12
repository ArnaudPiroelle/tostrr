package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.shipping.Ship
import app.tostrr.ui.common.portDisplayName
import app.tostrr.ui.theme.TostrrColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** When the ship reaches its next port, or the port it lies in, then the ports after it ([later]). */
@Composable
internal fun ShipArrivalCard(ship: Ship, later: List<String>, color: Color) {
    val port = ship.nextPort ?: return
    Surface(shape = RoundedCornerShape(16.dp), color = TostrrColors.SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(if (ship.atBerth) R.string.ship_arrival_berthed else R.string.ship_arrival_expected, portDisplayName(port)),
                style = MaterialTheme.typography.bodyMedium,
                color = TostrrColors.Muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                ship.eta?.let { shipEta(it, ship.etaTime) } ?: stringResource(R.string.ship_eta_unknown),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            val eta = ship.eta
            if (!ship.atBerth && eta != null) {
                Spacer(Modifier.height(4.dp))
                Text(daysUntil(eta), style = MaterialTheme.typography.bodyMedium)
            }
            if (later.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = TostrrColors.Border)
                Text(
                    stringResource(R.string.ship_then, later.joinToString(", ", transform = ::portDisplayName)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TostrrColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun daysUntil(eta: LocalDate): String = when (val days = ChronoUnit.DAYS.between(LocalDate.now(), eta).toInt()) {
    0 -> stringResource(R.string.ship_eta_today)
    1 -> stringResource(R.string.ship_eta_tomorrow)
    in 2..Int.MAX_VALUE -> pluralStringResource(R.plurals.ship_eta_in_days, days, days)
    else -> stringResource(R.string.ship_eta_passed)
}
