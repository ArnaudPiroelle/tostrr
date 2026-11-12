package app.tostrr.ui.ship.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.shipping.PortStatus
import app.tostrr.domain.shipping.TimelineEntry
import app.tostrr.domain.shipping.Voyage
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.ship.ShipViewModel.State
import app.tostrr.ui.theme.TostrrColors

/** The ship's details, the bottom sheet's content, scrolling when they don't fit even fully pulled up. */
@Composable
internal fun ShipSheet(state: State, voyage: Voyage, color: Color) {
    val later = state.timeline.filterIsInstance<TimelineEntry.Port>().filter { it.status == PortStatus.LATER }.map { it.name }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        ShipHeader(voyage.ship, state.candidates)
        Spacer(Modifier.height(16.dp))
        ShipArrivalCard(voyage.ship, later, color)
        Spacer(Modifier.height(12.dp))
        Row {
            Spacer(Modifier.padding(top = 6.dp).size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.ship_source, formatDateTime(state.snapshotAtEpochMs)),
                style = MaterialTheme.typography.bodySmall,
                color = TostrrColors.Muted,
            )
        }
        Spacer(Modifier.height(16.dp))
        ShipTimeline(state.timeline, voyage.ship, color)
        Spacer(Modifier.height(16.dp))
        ShipStats(voyage.ship, state.vinBlock)
    }
}
