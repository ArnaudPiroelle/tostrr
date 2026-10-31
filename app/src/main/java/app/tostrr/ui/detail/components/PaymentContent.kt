package app.tostrr.ui.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.order.Order

@Composable
internal fun PaymentContent(order: Order) {
    val info = order.paymentInfo
    if (info.isEmpty) return Empty(stringResource(R.string.detail_payment_empty))
    KeyValue(stringResource(R.string.detail_payment_status), info.status)
    KeyValue(stringResource(R.string.detail_payment_amount_due), info.amountDue)
    KeyValue(stringResource(R.string.detail_payment_amount_due_after_refund), info.amountDueAfterRefund)
    KeyValue(stringResource(R.string.detail_payment_amount_paid), info.amountPaid)
    KeyValue(stringResource(R.string.detail_payment_method), info.paymentMethod)
    KeyValue(stringResource(R.string.detail_payment_account_balance), info.accountBalance)
    KeyValue(stringResource(R.string.detail_payment_financed_amount), info.financedAmount)
    KeyValue(stringResource(R.string.detail_payment_finance_product), info.financeProduct)
    KeyValue(stringResource(R.string.detail_payment_finance_partner), info.financePartner)
    KeyValue(stringResource(R.string.detail_payment_monthly), info.monthlyPayment)
    KeyValue(stringResource(R.string.detail_payment_term), info.term)
    KeyValue(stringResource(R.string.detail_payment_rate), info.interestRate)
    KeyValue(stringResource(R.string.detail_payment_yearly_mileage), info.yearlyMileage)
    KeyValue(stringResource(R.string.detail_payment_approved_amount), info.approvedAmount)
}
