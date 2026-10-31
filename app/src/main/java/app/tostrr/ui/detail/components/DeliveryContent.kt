package app.tostrr.ui.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.order.Order

@Composable
internal fun DeliveryContent(order: Order, center: DeliveryCenter?) {
    order.deliveryCenter?.takeIf { it.isNotBlank() }?.let { DeliveryCenterRow(it, center) }
    KeyValue(stringResource(R.string.detail_delivery_method), order.deliveryMethod)
    KeyValue(stringResource(R.string.detail_delivery_appointment), order.appointment)
    KeyValue(stringResource(R.string.detail_delivery_window), order.deliveryWindow)
    KeyValue(stringResource(R.string.detail_delivery_eta), order.etaToDeliveryCenter)
    KeyValue(stringResource(R.string.detail_delivery_registration), order.expectedRegistration)
    if (listOf(order.deliveryCenter, order.deliveryMethod, order.appointment, order.deliveryWindow, order.etaToDeliveryCenter, order.expectedRegistration).all { it.isNullOrBlank() }) {
        Empty(stringResource(R.string.detail_delivery_empty))
    }
}
