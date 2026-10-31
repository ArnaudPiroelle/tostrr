package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.humanizeCode

@Composable
internal fun PaymentContent(order: Order) {
    val info = order.paymentInfo
    if (info.isEmpty) return Empty(stringResource(R.string.detail_payment_empty))
    KeyValue(stringResource(R.string.detail_payment_status), info.status?.let { code -> paymentStatusRes(code)?.let { stringResource(it) } ?: humanizeCode(code) })
    KeyValue(stringResource(R.string.detail_payment_amount_due), info.amountDue)
    KeyValue(stringResource(R.string.detail_payment_amount_due_after_refund), info.amountDueAfterRefund)
    KeyValue(stringResource(R.string.detail_payment_amount_paid), info.amountPaid)
    KeyValue(stringResource(R.string.detail_payment_method), info.paymentMethod)
    KeyValue(stringResource(R.string.detail_payment_account_balance), info.accountBalance)
    KeyValue(stringResource(R.string.detail_payment_financed_amount), info.financedAmount)
    KeyValue(
        stringResource(R.string.detail_payment_finance_product),
        info.financeProduct?.let { code -> financeProductRes(code)?.let { stringResource(it) } ?: humanizeCode(code) },
    )
    KeyValue(stringResource(R.string.detail_payment_finance_partner), info.financePartner)
    KeyValue(stringResource(R.string.detail_payment_monthly), info.monthlyPayment)
    KeyValue(stringResource(R.string.detail_payment_term), info.termMonths?.let { pluralStringResource(R.plurals.detail_payment_term_months, it, it) })
    KeyValue(stringResource(R.string.detail_payment_rate), info.interestRate)
    KeyValue(
        stringResource(R.string.detail_payment_yearly_mileage),
        info.yearlyMileage?.let { stringResource(R.string.detail_payment_yearly_mileage_value, it) },
    )
    KeyValue(stringResource(R.string.detail_payment_approved_amount), info.approvedAmount)
}

/** Tesla's final payment statuses in words; any other is shown humanized. */
@StringRes
private fun paymentStatusRes(code: String): Int? = when (code) {
    "MAKE_YOUR_FINAL_PAYMENT" -> R.string.detail_payment_status_due
    "COMPLETE", "COMPLETED" -> R.string.detail_payment_status_paid
    else -> null
}

@StringRes
private fun financeProductRes(code: String): Int? = when (code) {
    "CASH" -> R.string.detail_finance_cash
    "LOAN" -> R.string.detail_finance_loan
    "LEASE" -> R.string.detail_finance_lease
    else -> null
}
