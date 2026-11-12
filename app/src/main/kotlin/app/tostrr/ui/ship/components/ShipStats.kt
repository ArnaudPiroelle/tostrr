package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.shipping.Ship
import java.text.NumberFormat

/** How much of its route the ship has sailed, how deep it sits, and the VIN block it is followed for. */
@Composable
internal fun ShipStats(ship: Ship, vinBlock: String?) {
    val draught = ship.draughtMeters?.let {
        stringResource(R.string.ship_draught_value, NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(it))
    }
    Row(Modifier.fillMaxWidth()) {
        ShipStat(stringResource(R.string.ship_progress), ship.progress?.let { NumberFormat.getPercentInstance().format(it) }, Modifier.weight(1f))
        ShipStat(stringResource(R.string.ship_draught), draught, Modifier.weight(1f))
        ShipStat(stringResource(R.string.ship_vin_block), vinBlock, Modifier.weight(1f))
    }
}
