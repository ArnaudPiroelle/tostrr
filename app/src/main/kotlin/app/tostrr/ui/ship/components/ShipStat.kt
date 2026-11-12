package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.tostrr.ui.theme.TostrrColors

/** A figure under its [label]; a dash when unknown. */
@Composable
internal fun ShipStat(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TostrrColors.Muted)
        Text(value ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
