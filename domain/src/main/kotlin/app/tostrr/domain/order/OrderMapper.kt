package app.tostrr.domain.order

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Builds an [Order] from the two raw payloads returned by Tesla:
 *  - `order`   : one entry of `GET /api/1/users/orders`
 *  - `details` : the `/tasks` response for that order
 *
 * Both payloads are undocumented, so every access is defensive.
 */
object OrderMapper {

    fun map(
        order: JsonObject,
        details: JsonObject,
        fetchedAtEpochMs: Long,
        detectors: StepDetectors = StepDetectors.Default,
    ): Order {
        val tasks = details.obj("tasks")
        val scheduling = tasks.obj("scheduling")

        val reference = order.str("referenceNumber") ?: "?"
        val vin = vinIn(order.str("vin")) ?: VIN_PATHS.firstNotNullOfOrNull { vinIn(tasks.stringAt(it)) }
        val optionCodes = order.str("mktOptions").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val modelCode = order.str("modelCode")?.lowercase() ?: modelFromOptions(optionCodes)

        // The payload layout moves around between Tesla app versions, so dates are looked up by key name.
        val booked = tasks.deepFind("orderBookedDate") ?: tasks.deepFind("orderPlacedDate")
        val appointment = appointmentFrom(tasks)
        val window = deliveryWindowFrom(tasks)
        val eta = ETA_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) } ?: tasks.deepFind("etaToDeliveryCenter")
        val licensePlate = PayloadStepDetectors.PLATE_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) }
        val expectedReg = tasks.deepFind("expectedRegDate")

        val steps = listOf(
            OrderStep(StepKind.ORDERED, formatDate(booked), booked != null),
            OrderStep(StepKind.VIN_ASSIGNED, null, vin != null),
            OrderStep(StepKind.BUILT, null, detectors.carBuilt.isDone(order, tasks)),
            OrderStep(StepKind.DOCUMENTS_SENT, null, detectors.documentsSent.isDone(order, tasks)),
            OrderStep(StepKind.REGISTERED, formatDate(expectedReg), detectors.vehicleRegistered.isDone(order, tasks)),
            // Only a confirmed appointment is shown under the step; the delivery window lives in Order.deliverySlot.
            OrderStep(StepKind.DELIVERY, appointment, appointment != null),
        )

        return Order(
            reference = reference,
            status = order.str("orderStatus").orEmpty(),
            vin = vin,
            modelName = modelName(modelCode),
            optionCodes = optionCodes,
            imageUrl = imageUrl(modelCode, optionCodes),
            steps = steps,
            deliveryCenter = scheduling.str("deliveryAddressTitle"),
            deliveryWindow = window,
            deliverySlot = appointment ?: window?.let(::shortWindow)?.replace("\n", " – "),
            appointment = appointment,
            etaToDeliveryCenter = formatDate(eta),
            expectedRegistration = formatDate(expectedReg),
            fetchedAtEpochMs = fetchedAtEpochMs,
            raw = JsonObject(mapOf("order" to order, "details" to details)),
            licensePlate = licensePlate,
            appointmentAtEpochMs = APPOINTMENT_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) }?.let(::appointmentInstant),
            finalInvoiceReady = tasks.flag("finalPayment.data.hasFinalInvoice") || tasks.flag("finalPayment.data.hasActiveInvoice"),
            finalPaymentDone = tasks.flag("finalPayment.complete"),
            countryCode = order.str("countryCode") ?: scheduling.str("countryCode"),
            bookedOn = booked?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() },
            deliveryMethod = deliveryMethodFrom(tasks),
            vehicleInfo = OrderInfoMapper.vehicle(tasks, booked, licensePlate),
            paymentInfo = OrderInfoMapper.payment(tasks),
        )
    }

    private fun JsonObject.flag(path: String): Boolean =
        (at(path) as? JsonPrimitive)?.let { it.booleanOrNull ?: it.contentOrNull.equals("true", ignoreCase = true) } ?: false

    internal fun appointmentInstant(raw: String): Long? =
        parseAppointment(raw)?.toInstant()?.toEpochMilli()

    /** ISO instant, or local date-time read in the device time zone; null for anything else. */
    private fun parseAppointment(raw: String): ZonedDateTime? = runCatching {
        runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()) }
            .getOrElse { LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()) }
    }.getOrNull()

    private fun modelFromOptions(codes: List<String>): String? =
        codes.map { it.removePrefix("\$") }.firstOrNull { it.startsWith("MT") }?.let {
            when (it.getOrNull(2)) {
                '3' -> "m3"
                'Y' -> "my"
                'S' -> "ms"
                'X' -> "mx"
                else -> null
            }
        }

    private fun modelName(code: String?): String = when (code) {
        "m3" -> "Model 3"
        "my" -> "Model Y"
        "ms" -> "Model S"
        "mx" -> "Model X"
        "ct" -> "Cybertruck"
        else -> "Tesla"
    }

    private fun imageUrl(modelCode: String?, options: List<String>): String? {
        if (modelCode == null || options.isEmpty()) return null
        return "https://static-assets.tesla.com/configurator/compositor".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("context", "design_studio_2")
            ?.addQueryParameter("model", modelCode)
            ?.addQueryParameter("view", "STUD_3QTR")
            ?.addQueryParameter("size", "1200")
            ?.addQueryParameter("bkba_opt", "1")
            ?.addQueryParameter("options", options.joinToString(","))
            ?.build()?.toString()
    }

    /** Formats an ISO date/datetime as a short date in the device's language; returns the input untouched if it isn't ISO. */
    fun formatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(raw.take(10)).format(LocalDateFormats.date()) }.getOrDefault(raw)
    }

    /** "29 Novembre - 29 Décembre" -> "29 Nov\n29 Déc" so it fits under a stepper dot. */
    private fun shortWindow(window: String): String =
        window.split(WINDOW_SEPARATOR).joinToString("\n") { part ->
            part.split(' ').joinToString(" ") { if (it.length > 4 && it.all(Char::isLetter)) it.take(3) else it }
        }

    /**
     * First place that holds the VIN. A bare `vin` key elsewhere (the insurance
     * task has one) can hold another car's VIN, so it is not used.
     */
    private val VIN_PATHS = listOf(
        "deliveryDetails.regData.orderDetails.vin",
        "registration.orderDetails.vin",
        "registration.regData.orderDetails.vin",
        "finalPayment.data.vin",
    )

    /**
     * The VIN held by [raw], upper case, or null when it holds none: Tesla fills VIN fields with placeholders
     * (`VIN ##vin##`, `pending`) or a `VIN:` label before the car is matched, and a VIN never has I, O or Q.
     */
    internal fun vinIn(raw: String?): String? = raw?.uppercase()?.let { VIN.find(it) }?.value

    private val VIN = Regex("""(?<![A-Z0-9])[A-HJ-NPR-Z0-9]{17}(?![A-Z0-9])""")

    /** Where the VIN is read, in the same order, as paths of [Order.raw] and of the history. */
    internal val VIN_KEYS = listOf("order.vin") + VIN_PATHS.map { "details.tasks.$it" }

    /** Where the ETA is read first, before any other `etaToDeliveryCenter` in the tree. */
    private val ETA_PATHS = listOf("scheduling.etaToDeliveryCenter", "finalPayment.data.etaToDeliveryCenter")

    /** Start and end of the delivery window in the self-scheduling data, used when there is no display string. */
    private val WINDOW_RANGES = listOf(
        "Display.startDate" to "Display.endDate",
        "SelfScheduleDisplay.startDate" to "SelfScheduleDisplay.endDate",
        "deliveryWindow.startDate" to "deliveryWindow.endDate",
        "selfScheduleDetails.deliveryWindowStartDate" to "selfScheduleDetails.deliveryWindowEndDate",
    ).map { (start, end) -> "finalPayment.data.selfSchedulingDetails.$start" to "finalPayment.data.selfSchedulingDetails.$end" }

    private fun deliveryWindowFrom(tasks: JsonObject): String? =
        listOf("scheduling.deliveryWindowDisplay", "scheduling.Display").firstNotNullOfOrNull { tasks.stringAt(it)?.trim() }
            ?: WINDOW_RANGES.firstNotNullOfOrNull { (start, end) ->
                val from = formatDate(tasks.stringAt(start)) ?: return@firstNotNullOfOrNull null
                formatDate(tasks.stringAt(end))?.let { "$from – $it" } ?: from
            }

    /** Home delivery or pick-up, from the delivery type Tesla gives the handover. */
    private fun deliveryMethodFrom(tasks: JsonObject): DeliveryMethod? {
        val type = listOf("deliveryAcceptance.deliveryType", "scheduling.deliveryType").firstNotNullOfOrNull { tasks.stringAt(it) }?.uppercase()
        val toAddress = (tasks.at("deliveryAcceptance.isDeliveryToAddress") as? JsonPrimitive)?.booleanOrNull
        return when {
            type == DELIVERY_TO_ADDRESS || toAddress == true -> DeliveryMethod.HOME
            type?.startsWith("PICKUP") == true || toAddress == false -> DeliveryMethod.PICKUP
            else -> null
        }
    }

    private const val DELIVERY_TO_ADDRESS = "DELIVERY_TO_ADDRESS"

    /** Fallback sources for the appointment, local time before UTC; `scheduling` is tried first. */
    private val APPOINTMENT_PATHS = listOf(
        "registration.regData.deliveryAppointment.appointmentDate",
        "deliveryAcceptance.appointmentDate",
        "registration.regData.deliveryAppointment.appointmentDateUtc",
        "deliveryAcceptance.appointmentDateUtc",
    )

    private val WINDOW_SEPARATOR = Regex("""\s*[-–]\s*""")
    private val WHITESPACE = Regex("""\s+""")

    private fun appointmentFrom(tasks: JsonObject): String? {
        // Already a display string in the user's language, so it is kept as is.
        tasks.obj("scheduling").str("deliveryAppointmentDate")?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return it.replace(WHITESPACE, " ") }
        return APPOINTMENT_PATHS.firstNotNullOfOrNull { tasks.stringAt(it)?.trim()?.takeIf(String::isNotEmpty) }
            ?.let(::formatAppointment)
    }

    /** ISO instants and local date-times become a date and time in the device's language and time zone; anything else is kept. */
    internal fun formatAppointment(raw: String): String = parseAppointment(raw)?.format(LocalDateFormats.dateTime()) ?: raw
}

internal fun JsonObject?.obj(key: String): JsonObject =
    (this?.get(key) as? JsonObject) ?: JsonObject(emptyMap())

internal fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.contentOrNull

/** Element at a dotted [path], matching keys case-insensitively. */
internal fun JsonObject.at(path: String): JsonElement? {
    var node: JsonElement = this
    for (part in path.split('.')) {
        val obj = node as? JsonObject ?: return null
        node = obj.entries.firstOrNull { it.key.equals(part, ignoreCase = true) }?.value ?: return null
    }
    return node
}

/** Non-blank string at a dotted [path], or null. */
internal fun JsonObject.stringAt(path: String): String? =
    ((at(path) as? JsonPrimitive)?.takeIf { it.isString })?.content?.takeIf { it.isNotBlank() }

/** First non-blank scalar stored under [key] anywhere in the tree (depth-first). */
internal fun JsonElement.deepFind(key: String): String? = when (this) {
    is JsonObject -> (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: values.firstNotNullOfOrNull { it.deepFind(key) }
    is JsonArray -> firstNotNullOfOrNull { it.deepFind(key) }
    else -> null
}
