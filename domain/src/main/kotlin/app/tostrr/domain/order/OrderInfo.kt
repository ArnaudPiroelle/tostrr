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
    val odometer: String? = null,
    val routingLocation: String? = null,
) {
    val isEmpty: Boolean get() = this == VehicleInfo()
}

/** Where the registration stands, as the buyer would put it. */
enum class RegistrationStage {
    /** Tesla still needs the buyer's details. */
    DETAILS_NEEDED,
    DETAILS_SENT,
    PREPARING_DOCUMENTS,
    /** Tesla waits for a proof from the buyer. */
    AWAITING_PROOF,
    DOCUMENTS_SENT,
    AWAITING_PLATE,
    REGISTERED,
}

/** What the buyer still has to do for the registration, in the Tesla app. */
enum class RegistrationTodo { DRIVING_LICENCE, CO_OWNER, IDENTITY }

/**
 * The registration, from the `registration` task (and the identity checks of `deliveryDetails`). The plate and the
 * expected day are on [Order] ([Order.licensePlate], [Order.expectedRegistration]). Every field is null until Tesla
 * sends it.
 */
data class RegistrationInfo(
    val stage: RegistrationStage? = null,
    /** Who the car is registered to: the company for a company registration, else the owner. */
    val holder: String? = null,
    val coHolder: String? = null,
    val todo: List<RegistrationTodo> = emptyList(),
) {
    val isEmpty: Boolean get() = this == RegistrationInfo()
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

/** What Tesla lets a trade-in carry over to the new car. */
enum class TradeInTransfer { FSD, ENHANCED_AUTOPILOT, FREE_SUPERCHARGING }

/** What Tesla waits for the buyer to do about the trade-in, in the Tesla app. */
enum class TradeInAction { VIEW_OFFER, ACCEPT_OFFER }

/** The car traded in and Tesla's offer for it, from the `tradeIn` task. Every field is null until Tesla sends it. */
data class TradeInInfo(
    /** The buyer's answer to "do you have a car to trade in?", as Tesla gives it: `No` means none. */
    val intent: String? = null,
    /** Year, make and model, the parts Tesla gives. */
    val vehicle: String? = null,
    val mileage: String? = null,
    /** Only while there is no [finalOffer]. */
    val estimatedValue: String? = null,
    val finalOffer: String? = null,
    val offerExpires: String? = null,
    /** Null when Tesla waits for nothing, or for a step the app doesn't know. */
    val action: TradeInAction? = null,
    val transfers: List<TradeInTransfer> = emptyList(),
) {
    val isEmpty: Boolean get() = this == TradeInInfo()

    /** Tesla has a trade-in task the buyer didn't answer "no" to. */
    val hasTradeIn: Boolean get() = !isEmpty && !intent.equals(NO_TRADE_IN, ignoreCase = true)

    /** Nothing to show besides the trade-in itself: Tesla hasn't valued the car yet. */
    val hasNoOffer: Boolean get() = this == TradeInInfo(intent = intent)

    private companion object {
        const val NO_TRADE_IN = "No"
    }
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

    fun vehicle(tasks: JsonObject, bookedDate: String?): VehicleInfo {
        val orderDetails = "registration.orderDetails"
        return VehicleInfo(
            reservationDate = OrderMapper.formatDate(tasks.stringAt("$orderDetails.reservationDate")),
            bookedDate = OrderMapper.formatDate(bookedDate),
            odometer = tasks.number("$orderDetails.vehicleOdometer")?.let { km ->
                val unit = tasks.stringAt("$orderDetails.vehicleOdometerType")?.let { if (it.startsWith("MI", ignoreCase = true)) "mi" else "km" } ?: "km"
                "${plain(km)} $unit"
            },
            routingLocation = tasks.text("$orderDetails.vehicleRoutingLocation"),
        )
    }

    /**
     * The stage comes from Tesla's registration alert when there is one (values seen in other apps, not yet in a
     * payload here), else from the plate, else from whether the buyer's part of the task is done.
     */
    fun registration(tasks: JsonObject, licensePlate: String?): RegistrationInfo {
        val task = "registration"
        val root = tasks.at(task) as? JsonObject ?: return RegistrationInfo()
        if ((root.at("enabled") as? JsonPrimitive)?.booleanOrNull == false) return RegistrationInfo()
        val alert = REGISTRATION_ALERTS.firstNotNullOfOrNull { tasks.text(it) }?.uppercase()
        val stage = when {
            licensePlate != null || alert == "COMPLETE" -> RegistrationStage.REGISTERED
            alert != null && alert in ALERT_STAGES -> ALERT_STAGES.getValue(alert)
            (root.at("complete") as? JsonPrimitive)?.booleanOrNull == true -> RegistrationStage.DETAILS_SENT
            else -> RegistrationStage.DETAILS_NEEDED
        }
        fun person(path: String) = listOfNotNull(tasks.text("$path.firstName"), tasks.text("$path.lastName")).joinToString(" ").takeIf { it.isNotEmpty() }
        val owner = person("$task.regData.owner.user")
        val company = tasks.text("$task.regData.registrantType").equals("COMPANY", ignoreCase = true)
        val identity = IDENTITY_PATHS.mapNotNull { tasks.text(it)?.uppercase() }.filter { it !in IDENTITY_IGNORED }
        return RegistrationInfo(
            stage = stage,
            holder = (if (company) tasks.text("deliveryDetails.companyName") else null) ?: owner,
            coHolder = person("$task.regData.coOwner.user"),
            todo = listOfNotNull(
                RegistrationTodo.DRIVING_LICENCE.takeIf { (root.at("hasDLDetails") as? JsonPrimitive)?.booleanOrNull == false },
                RegistrationTodo.CO_OWNER.takeIf { (root.at("isPendingCoSigner") as? JsonPrimitive)?.booleanOrNull == true },
                RegistrationTodo.IDENTITY.takeIf { identity.any { it !in IDENTITY_DONE } },
            ),
        )
    }

    private val REGISTRATION_ALERTS = listOf(
        "registration.regData.regAlerts.registration",
        "registration.alertStatuses.registration",
        "deliveryDetails.regData.regAlerts.registration",
    )

    private val ALERT_STAGES = mapOf(
        "INCOMPLETE" to RegistrationStage.DETAILS_NEEDED,
        "PREPAREDOCS" to RegistrationStage.PREPARING_DOCUMENTS,
        "AWAITING_CONFIRMATION" to RegistrationStage.AWAITING_PROOF,
        "APPT_PENDING" to RegistrationStage.DOCUMENTS_SENT,
        "PROOF_OF_REG_PENDING" to RegistrationStage.AWAITING_PLATE,
    )

    private val IDENTITY_PATHS = listOf(
        "deliveryDetails.verificationStatus",
        "deliveryDetails.fourthline.validationStatus",
        "deliveryDetails.fourthline.verificationStatus",
    )
    private val IDENTITY_IGNORED = setOf("DEFAULT", "IGNORE", "NULL")
    private val IDENTITY_DONE = setOf("COMPLETE", "COMPLETED", "SIGNED", "SUCCESS", "VERIFIED")

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

    /**
     * The `tradeIn` task, with the final payment's copy of the intent as fallback. A disabled task gives nothing. The
     * car's own fields are under `tradeInVehicle` or `currentVehicle`, depending on the stage; none was seen filled in
     * a real payload yet, so those paths are unverified.
     */
    fun tradeIn(tasks: JsonObject): TradeInInfo {
        val task = "tradeIn"
        if ((tasks.at("$task.enabled") as? JsonPrimitive)?.booleanOrNull == false) return TradeInInfo()
        val currency = (listOf("$task.currencyCode") + CURRENCY_PATHS).firstNotNullOfOrNull { tasks.stringAt(it) }
        fun car(key: String) = TRADE_IN_CARS.firstNotNullOfOrNull { tasks.text("$it.$key") }
        fun carMoney(vararg keys: String) = keys.firstNotNullOfOrNull { key -> TRADE_IN_CARS.firstNotNullOfOrNull { tasks.at("$it.$key").moneyIn(currency) } }
        val finalOffer = carMoney("finalOffer")
        val canAccept = (tasks.at("$task.canAcceptOffer") as? JsonPrimitive)?.booleanOrNull == true
        return TradeInInfo(
            intent = tasks.text("$task.tradeInIntent") ?: tasks.text("finalPayment.data.tradeInIntent"),
            vehicle = listOfNotNull(car("Year"), car("Make"), car("Model")).joinToString(" ").takeIf { it.isNotEmpty() },
            mileage = TRADE_IN_CARS.firstNotNullOfOrNull { root ->
                tasks.number("$root.Mileage")?.let { distance ->
                    val unit = if (tasks.text("$root.OdometerType")?.startsWith("MI", ignoreCase = true) == true) "mi" else "km"
                    // A no-break space keeps the unit on the number's line.
                    "${plain(distance)}\u00A0$unit"
                }
            },
            estimatedValue = if (finalOffer != null) null else carMoney("SelectedValuation.Value", "SelectedValuation.EstimatedOffer", "TradeInCredit"),
            finalOffer = finalOffer,
            offerExpires = OrderMapper.formatDate(car("SelectedValuation.ValuationExpireDate") ?: car("SelectedValuation.EstimateDate")),
            action = when {
                canAccept -> TradeInAction.ACCEPT_OFFER
                else -> when (tasks.text("$task.nextStep")?.lowercase()) {
                    "acceptoffer" -> TradeInAction.ACCEPT_OFFER
                    "viewoffer" -> TradeInAction.VIEW_OFFER
                    else -> null
                }
            },
            transfers = TRADE_IN_TRANSFERS.filter { (key, _) ->
                listOf("enabled", "isEligible").any { (tasks.at("$task.transfers.$key.$it") as? JsonPrimitive)?.booleanOrNull == true }
            }.map { it.second },
        )
    }

    private val TRADE_IN_CARS = listOf("tradeIn.tradeInVehicle", "tradeIn.currentVehicle")

    private val TRADE_IN_TRANSFERS = listOf(
        "FSD_TRANSFER" to TradeInTransfer.FSD,
        "EAP_TRANSFER" to TradeInTransfer.ENHANCED_AUTOPILOT,
        "SUPER_CHARGE_TRANSFER" to TradeInTransfer.FREE_SUPERCHARGING,
    )

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
