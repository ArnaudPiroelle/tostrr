package com.tostrr.domain

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
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        val vin = (order.str("vin") ?: VIN_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) })?.takeIf { it.isNotBlank() }
        val optionCodes = order.str("mktOptions").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val modelCode = order.str("modelCode")?.lowercase() ?: modelFromOptions(optionCodes)

        // The payload layout moves around between Tesla app versions, so dates are looked up by key name.
        val booked = tasks.deepFind("orderBookedDate") ?: tasks.deepFind("orderPlacedDate")
        val appointment = appointmentFrom(tasks)
        val window = scheduling.str("deliveryWindowDisplay")?.takeIf { it.isNotBlank() }
        val eta = tasks.deepFind("etaToDeliveryCenter")
        val expectedReg = tasks.deepFind("expectedRegDate")

        val steps = listOf(
            OrderStep("Commande passée", formatDate(booked), booked != null, "Votre commande Tesla est bien enregistrée"),
            OrderStep("VIN attribué", null, vin != null, "Votre Tesla a maintenant un VIN attribué"),
            OrderStep("Production terminée", null, detectors.carBuilt.isDone(order, tasks), "Votre Tesla est sortie de production"),
            OrderStep("Documents envoyés", null, detectors.documentsSent.isDone(order, tasks), "Les documents de votre Tesla ont été envoyés"),
            OrderStep("Véhicule immatriculé", formatDate(expectedReg), detectors.vehicleRegistered.isDone(order, tasks), "Votre Tesla est maintenant immatriculée"),
            // Only a confirmed appointment is shown under the step; the delivery window lives in Order.deliverySlot.
            OrderStep("Livraison", appointment, appointment != null, "Votre rendez-vous de livraison est confirmé"),
        )

        return Order(
            reference = reference,
            status = order.str("orderStatus").orEmpty(),
            vin = vin,
            modelName = modelName(modelCode),
            optionCodes = optionCodes,
            imageUrl = imageUrl(modelCode, optionCodes),
            bookedDate = formatDate(booked),
            steps = steps,
            deliveryCenter = scheduling.str("deliveryAddressTitle"),
            deliveryWindow = window,
            deliverySlot = appointment ?: window?.let(::shortWindow)?.replace("\n", " – "),
            appointment = appointment,
            etaToDeliveryCenter = formatDate(eta),
            expectedRegistration = formatDate(expectedReg),
            fetchedAtEpochMs = fetchedAtEpochMs,
            raw = JsonObject(mapOf("order" to order, "details" to details)),
            licensePlate = PayloadStepDetectors.PLATE_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) },
            appointmentAtEpochMs = APPOINTMENT_PATHS.firstNotNullOfOrNull { tasks.stringAt(it) }?.let(::appointmentInstant),
            finalInvoiceReady = tasks.flag("finalPayment.data.hasFinalInvoice") || tasks.flag("finalPayment.data.hasActiveInvoice"),
            finalPaymentDone = tasks.flag("finalPayment.complete"),
            countryCode = order.str("countryCode") ?: scheduling.str("countryCode"),
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

    private val dateOut = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.FRANCE)

    /** Formats an ISO date/datetime as dd/MM/yy; returns the input untouched if it isn't ISO. */
    fun formatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(raw.take(10)).format(dateOut) }.getOrDefault(raw)
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

    /** Fallback sources for the appointment, local time before UTC; `scheduling` is tried first. */
    private val APPOINTMENT_PATHS = listOf(
        "registration.regData.deliveryAppointment.appointmentDate",
        "deliveryAcceptance.appointmentDate",
        "registration.regData.deliveryAppointment.appointmentDateUtc",
        "deliveryAcceptance.appointmentDateUtc",
    )

    private val WINDOW_SEPARATOR = Regex("""\s*[-–]\s*""")
    private val WHITESPACE = Regex("""\s+""")

    private val appointmentOut = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE)

    private fun appointmentFrom(tasks: JsonObject): String? {
        // Already a display string in the user's language, so it is kept as is.
        tasks.obj("scheduling").str("deliveryAppointmentDate")?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return it.replace(WHITESPACE, " ") }
        return APPOINTMENT_PATHS.firstNotNullOfOrNull { tasks.stringAt(it)?.trim()?.takeIf(String::isNotEmpty) }
            ?.let(::formatAppointment)
    }

    /** ISO instants and local date-times become `dd/MM/yyyy HH:mm` in the device time zone; anything else is kept. */
    internal fun formatAppointment(raw: String): String = parseAppointment(raw)?.format(appointmentOut) ?: raw
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
