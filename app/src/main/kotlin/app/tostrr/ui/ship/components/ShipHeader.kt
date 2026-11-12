package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.shipping.Ship
import app.tostrr.ui.common.portDisplayName
import app.tostrr.ui.common.shipDisplayName
import app.tostrr.ui.theme.TostrrColors

/** Whether the ship sails or lies in port, its name, line and route. */
@Composable
internal fun ShipHeader(ship: Ship, candidates: Int) {
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ship.atBerth) {
                ShipStatusChip(stringResource(R.string.ship_berthed), TostrrColors.Muted)
            } else {
                ShipStatusChip(stringResource(R.string.ship_sailing), TostrrColors.Positive)
            }
            if (candidates > 1) ShipStatusChip(stringResource(R.string.ship_candidate, candidates), TostrrColors.Accent)
        }
        Spacer(Modifier.height(8.dp))
        Text(shipDisplayName(ship.name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        subtitle(ship)?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
        }
    }
}

/** `Hyundai Glovis · Nangang → Zeebrugge via Singapore, Mauritius`, without what the site doesn't give. */
@Composable
private fun subtitle(ship: Ship): String? {
    val ports = ship.route.map(::portDisplayName)
    val route = when {
        ports.size > 2 -> stringResource(R.string.ship_route_via, ports.first(), ports.last(), ports.subList(1, ports.lastIndex).joinToString(", "))
        ports.size == 2 -> stringResource(R.string.ship_route, ports[0], ports[1])
        else -> ports.firstOrNull()
    }
    val operator = ship.operator
    return when {
        operator != null && route != null -> stringResource(R.string.ship_subtitle, operator, route)
        else -> operator ?: route
    }
}
