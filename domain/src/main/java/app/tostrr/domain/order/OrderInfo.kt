package app.tostrr.domain.order

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** The order and the car itself, as display strings. Every field is null until Tesla sends it. */
data class VehicleInfo(
    val reservationDate: String? = null,
    val bookedDate: String? = null,
    val licensePlate: String? = null,
    val odometer: String? = null,
    val routingLocation: String? = null,
    val registrationStatus: String? = null,
) {
    val isEmpty: Boolean get() = this == VehicleInfo()
}

/**
 * Final payment and financing, as display strings, except the codes the screens word ([status], [financeProduct]:
 * Tesla's code, upper case) and the units they add ([termMonths], [yearlyMileage] in km). Every field is null until
 * Tesla sends it.
 */
data class PaymentInfo(
    val status: String? = null,
    val amountDue: String? = null,
    val amountDueAfterRefund: String? = null,
    val amountPaid: String? = null,
    val paymentMethod: String? = null,
    val accountBalance: String? = null,
    val financedAmount: String? = null,
    val financeProduct: String? = null,
    val financePartner: String? = null,
    val monthlyPayment: String? = null,
    val termMonths: Int? = null,
    val interestRate: String? = null,
    val yearlyMileage: String? = null,
    val approvedAmount: String? = null,
) {
    val isEmpty: Boolean get() = this == PaymentInfo()
}

/** `MAKE_YOUR_FINAL_PAYMENT` -> `Make your final payment`, for codes without a known label. */
fun humanizeCode(code: String): String =
    if (code.any(Char::isLowerCase) || ' ' in code) code
    else code.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

/**
 * Reads the fields shown besides the steps: order status and dates, vehicle, payment and
 * financing. Paths are below `details.tasks`; most are only filled once a car is matched to the order.
 */
internal object OrderInfoMapper {

    fun vehicle(tasks: JsonObject, bookedDate: String?, licensePlate: String?): VehicleInfo {
        val orderDetails = "registration.orderDetails"
        return VehicleInfo(
            reservationDate = OrderMapper.formatDate(tasks.stringAt("$orderDetails.reservationDate")),
            bookedDate = OrderMapper.formatDate(bookedDate),
            licensePlate = licensePlate,
            odometer = tasks.number("$orderDetails.vehicleOdometer")?.let { km ->
                val unit = tasks.stringAt("$orderDetails.vehicleOdometerType")?.let { if (it.startsWith("MI", ignoreCase = true)) "mi" else "km" } ?: "km"
                "${plain(km)} $unit"
            },
            routingLocation = tasks.text("$orderDetails.vehicleRoutingLocation"),
            registrationStatus = tasks.text("$orderDetails.registrationStatus")?.let(::humanize),
        )
    }

    fun payment(tasks: JsonObject): PaymentInfo {
        val data = "finalPayment.data"
        val finance = "$data.financingDetails.teslaFinanceDetails"
        val currency = CURRENCY_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) }
        fun money(vararg paths: String) = paths.firstNotNullOfOrNull { tasks.at(it).moneyIn(currency) }
        val paid = tasks.at("$data.paymentDetails")
        return PaymentInfo(
            status = tasks.text("finalPayment.status")?.uppercase(),
            amountDue = money("$data.amountDue", "finalPayment.amountDue"),
            amountDueAfterRefund = money("$data.amountDueAfterRefund"),
            amountPaid = paymentEntries(paid).mapNotNull { (it as? JsonObject)?.at("amountPaid")?.number() }
                .takeIf { it.isNotEmpty() }?.sum()?.let { formatMoney(it, currency) },
            paymentMethod = paymentEntries(paid).mapNotNull { (it as? JsonObject)?.text("paymentType") }
                .distinct().takeIf { it.isNotEmpty() }?.joinToString(", ") { humanize(it) },
            accountBalance = money("$data.accountBalance"),
            financedAmount = money("$data.amountDueFinancier"),
            financeProduct = tasks.text("$data.financingDetails.financialProductType")?.uppercase(),
            // The agreements task names the partner as soon as a loan or lease is chosen, long before the final payment.
            financePartner = tasks.text("$finance.financePartnerName") ?: tasks.text("agreements.teslaFinanceDetails.partnerName"),
            monthlyPayment = money("$finance.monthlyPayment"),
            termMonths = tasks.number("$finance.termsInMonths")?.toInt(),
            interestRate = tasks.number("$finance.interestRate")?.let { "${decimal(it)} %" },
            yearlyMileage = tasks.number("$finance.mileage")?.let(::plain),
            approvedAmount = money("$finance.approvedLoanAmount"),
        )
    }

    /** `paymentDetails` is a single payment or a list of them. */
    private fun paymentEntries(element: JsonElement?): List<JsonElement> = when (element) {
        is JsonArray -> element
        is JsonObject -> listOf(element)
        else -> emptyList()
    }

    /** A number, or a string already formatted by Tesla (kept as is). */
    private fun JsonElement?.moneyIn(currency: String?): String? {
        val primitive = this as? JsonPrimitive ?: return null
        primitive.number()?.let { return formatMoney(it, currency) }
        return primitive.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && primitive.isString }
    }

    private fun JsonElement?.number(): Double? {
        val primitive = this as? JsonPrimitive ?: return null
        if (primitive.booleanOrNull != null) return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.replace(',', '.')?.trim()?.toDoubleOrNull()
    }

    private fun JsonObject.number(path: String): Double? = at(path).number()

    /** Non-blank scalar at [path], numbers included. */
    private fun JsonObject.text(path: String): String? =
        (at(path) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    /** In Tesla's currency, written the device's way: `5 000 €` in French, `€5,000` in English. */
    internal fun formatMoney(amount: Double, currencyCode: String?, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { Currency.getInstance(currencyCode ?: "EUR") }.getOrNull()?.let { format.currency = it }
        if (amount % 1.0 == 0.0) format.maximumFractionDigits = 0
        return format.format(amount)
    }

    private fun plain(value: Double): String =
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(Math.round(value))

    private fun decimal(value: Double): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(BigDecimal.valueOf(value))

    private fun humanize(code: String): String = humanizeCode(code)

    private val CURRENCY_PATHS = listOf(
        "finalPayment.currencyFormat.currencyCode",
        "finalPayment.data.currencyCode",
        "tradeIn.currencyCode",
    )
}
