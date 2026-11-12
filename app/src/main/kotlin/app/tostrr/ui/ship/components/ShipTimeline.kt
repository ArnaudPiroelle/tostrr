package app.tostrr.ui.ship.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.shipping.PortStatus
import app.tostrr.domain.shipping.Ship
import app.tostrr.domain.shipping.TimelineEntry
import app.tostrr.ui.common.portDisplayName
import app.tostrr.ui.theme.TostrrColors
import java.text.NumberFormat

/** The ship's ports in order and where it is among them, the way sailed drawn in [color]. */
@Composable
internal fun ShipTimeline(timeline: List<TimelineEntry>, ship: Ship, color: Color) {
    val track = TostrrColors.Track
    fun reached(entry: TimelineEntry) = entry !is TimelineEntry.Port || entry.status in REACHED
    Column {
        timeline.forEachIndexed { index, entry ->
            val above = if (index == 0) null else if (reached(entry)) color else track
            val below = timeline.getOrNull(index + 1)?.let { if (reached(it)) color else track }
            when (entry) {
                is TimelineEntry.AtSea -> ShipTimelineRow(
                    title = stringResource(R.string.ship_at_sea),
                    trailing = entry.progress?.let { stringResource(R.string.ship_route_share, NumberFormat.getPercentInstance().format(it)) },
                    dot = TimelineDot.CURRENT,
                    color = color,
                    lineAbove = above,
                    lineBelow = below,
                )
                is TimelineEntry.Port -> ShipTimelineRow(
                    title = portDisplayName(entry.name),
                    trailing = when (entry.status) {
                        PortStatus.DEPARTED -> stringResource(R.string.ship_departed)
                        PortStatus.PASSED -> stringResource(R.string.ship_called)
                        PortStatus.BERTHED -> stringResource(R.string.ship_in_port_now)
                        PortStatus.NEXT -> ship.eta?.let { stringResource(R.string.ship_expected, shipEta(it, ship.etaTime)) }
                        PortStatus.LATER -> null
                    },
                    dot = when (entry.status) {
                        PortStatus.DEPARTED, PortStatus.PASSED -> TimelineDot.LEFT
                        PortStatus.BERTHED -> TimelineDot.CURRENT
                        PortStatus.NEXT, PortStatus.LATER -> TimelineDot.AHEAD
                    },
                    color = color,
                    lineAbove = above,
                    lineBelow = below,
                )
            }
        }
    }
}

private val REACHED = setOf(PortStatus.DEPARTED, PortStatus.PASSED, PortStatus.BERTHED)
