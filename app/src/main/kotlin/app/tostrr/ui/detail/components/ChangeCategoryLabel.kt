package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import app.tostrr.R
import app.tostrr.domain.history.ChangeCategory

/** A change category in words, on the history's filter chips and above each change. */
@get:StringRes
internal val ChangeCategory.labelRes: Int
    get() = when (this) {
        ChangeCategory.ORDER -> R.string.detail_history_category_order
        ChangeCategory.VIN -> R.string.detail_history_category_vin
        ChangeCategory.DELIVERY_WINDOW -> R.string.detail_history_category_window
        ChangeCategory.APPOINTMENT -> R.string.detail_history_category_appointment
        ChangeCategory.DELIVERY -> R.string.detail_history_category_delivery
        ChangeCategory.REGISTRATION -> R.string.detail_history_category_registration
        ChangeCategory.AGREEMENTS -> R.string.detail_history_category_agreements
        ChangeCategory.FINANCING -> R.string.detail_history_category_financing
        ChangeCategory.PAYMENT -> R.string.detail_history_category_payment
        ChangeCategory.TRADE_IN -> R.string.detail_history_category_trade_in
        ChangeCategory.INSURANCE -> R.string.detail_history_category_insurance
        ChangeCategory.OTHER -> R.string.detail_history_category_other
    }
