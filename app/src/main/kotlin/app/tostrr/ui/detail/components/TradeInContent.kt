package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.TradeInAction
import app.tostrr.domain.order.TradeInTransfer

/** The car traded in, Tesla's offer for it, and what is left to do while the order is under way. */
@Composable
internal fun TradeInContent(order: Order) {
    val info = order.tradeInInfo
    if (info.hasNoOffer) return Empty(stringResource(R.string.detail_trade_in_no_offer))
    KeyValue(stringResource(R.string.detail_trade_in_vehicle), info.vehicle)
    KeyValue(stringResource(R.string.detail_trade_in_mileage), info.mileage)
    KeyValue(stringResource(R.string.detail_trade_in_final_offer), info.finalOffer)
    KeyValue(stringResource(R.string.detail_trade_in_estimate), info.estimatedValue)
    KeyValue(stringResource(R.string.detail_trade_in_offer_expires), info.offerExpires)
    if (order.awaitingDelivery) KeyValue(stringResource(R.string.detail_trade_in_action), info.action?.let { stringResource(it.labelRes) })
    KeyValue(
        stringResource(R.string.detail_trade_in_transfers),
        info.transfers.map { stringResource(it.labelRes) }.takeIf { it.isNotEmpty() }?.joinToString(", "),
    )
}

@get:StringRes
private val TradeInAction.labelRes: Int
    get() = when (this) {
        TradeInAction.VIEW_OFFER -> R.string.detail_trade_in_action_view_offer
        TradeInAction.ACCEPT_OFFER -> R.string.detail_trade_in_action_accept_offer
    }

@get:StringRes
private val TradeInTransfer.labelRes: Int
    get() = when (this) {
        TradeInTransfer.FSD -> R.string.detail_trade_in_transfer_fsd
        TradeInTransfer.ENHANCED_AUTOPILOT -> R.string.detail_trade_in_transfer_eap
        TradeInTransfer.FREE_SUPERCHARGING -> R.string.detail_trade_in_transfer_supercharging
    }
