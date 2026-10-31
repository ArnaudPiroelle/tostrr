package app.tostrr.ui.orders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.common.StepTracker
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors
import coil3.compose.AsyncImage

private const val CARD_IMAGE_ZOOM = 1.8f

@Composable
internal fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TostrrColors.Surface),
        border = BorderStroke(0.5.dp, TostrrColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            stringResource(R.string.order_label),
                            Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TostrrColors.Muted,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.order_reference, order.reference),
                            Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(order.modelName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        stringResource(R.string.order_vin, order.vin ?: stringResource(R.string.order_vin_unknown)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TostrrColors.Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    order.deliverySlot?.let { slot ->
                        Text(
                            stringResource(R.string.order_slot, slot),
                            style = MaterialTheme.typography.labelMedium,
                            color = TostrrColors.Muted,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = order.imageUrl,
                    contentDescription = order.modelName,
                    // Layout size is unchanged; the zoom only affects drawing. The image's transparent margins
                    // absorb the overflow, so the car grows without pushing any other content around.
                    modifier = Modifier.width(140.dp).height(76.dp).graphicsLayer(scaleX = CARD_IMAGE_ZOOM, scaleY = CARD_IMAGE_ZOOM),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(16.dp))
            StepTracker(order)
            Text(
                stringResource(R.string.order_updated_at, formatDateTime(order.fetchedAtEpochMs)),
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TostrrColors.Muted,
                textAlign = TextAlign.End,
            )
        }
    }
}
