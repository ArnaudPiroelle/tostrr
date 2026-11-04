package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors
import coil3.compose.AsyncImage

private val HERO_IMAGE_HEIGHT = 175.dp
private const val HERO_IMAGE_ZOOM = 1.7f

@Composable
internal fun Hero(order: Order) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(TostrrColors.Accent.copy(alpha = 0.10f), MaterialTheme.colorScheme.background),
                ),
            ),
    ) {
        Column(Modifier.statusBarsPadding().padding(bottom = 8.dp)) {
            // Room for the pinned top bar (StickyTopBar); the car starts right below it.
            Spacer(Modifier.height(TOP_BAR_HEIGHT))
            AsyncImage(
                model = order.imageUrl,
                contentDescription = order.modelName,
                // The compositor image has transparent margins on every side (the car fills ~half of it).
                // Zoom it from the centre and let the box crop the empty edges.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HERO_IMAGE_HEIGHT)
                    .clipToBounds()
                    .graphicsLayer(scaleX = HERO_IMAGE_ZOOM, scaleY = HERO_IMAGE_ZOOM),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(order.modelName, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    if (order.status.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(50), color = TostrrColors.Accent.copy(alpha = 0.2f)) {
                            Text(
                                statusLabelRes(order.status)?.let { stringResource(it) } ?: order.status,
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TostrrColors.OnSurface,
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.order_title, order.reference),
                    style = MaterialTheme.typography.titleMedium,
                    color = TostrrColors.Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                VinRow(order.vin, Modifier.padding(top = 4.dp))
                Text(
                    stringResource(R.string.order_updated_at, formatDateTime(order.fetchedAtEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TostrrColors.Muted,
                )
            }
        }
    }
}

/** The known statuses in words; any other is shown as Tesla sends it. */
@StringRes
private fun statusLabelRes(status: String): Int? = when (status) {
    "BOOKED" -> R.string.order_status_booked
    "DELIVERED" -> R.string.order_status_delivered
    "CANCELLED" -> R.string.order_status_cancelled
    else -> null
}
