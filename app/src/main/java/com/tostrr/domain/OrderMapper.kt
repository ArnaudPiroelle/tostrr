package com.tostrr.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.LocalDate
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

    fun map(order: JsonObject, details: JsonObject, fetchedAtEpochMs: Long): Order {
        val tasks = details.obj("tasks")
        val scheduling = tasks.obj("scheduling")

        val reference = order.str("referenceNumber") ?: "?"
        val vin = (order.str("vin") ?: tasks.deepFind("vin"))?.takeIf { it.isNotBlank() }
        val optionCodes = order.str("mktOptions").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val modelCode = order.str("modelCode")?.lowercase() ?: modelFromOptions(optionCodes)

        // The payload layout moves around between Tesla app versions, so dates are looked up by key name.
        val booked = tasks.deepFind("orderBookedDate") ?: tasks.deepFind("orderPlacedDate")
        val odometer = tasks.deepFind("vehicleOdometer")?.toDoubleOrNull()
        val appointment = scheduling.str("deliveryAppointmentDate")?.trim()?.takeIf { it.isNotEmpty() }
            ?.split(Regex("\\s+"))?.joinToString(" ")
        val window = scheduling.str("deliveryWindowDisplay")?.takeIf { it.isNotBlank() }
        val eta = tasks.deepFind("etaToDeliveryCenter")
        val expectedReg = tasks.deepFind("expectedRegDate")

        val steps = listOf(
            OrderStep("Commande passée", formatDate(booked), done = booked != null),
            OrderStep("VIN attribué", null, done = vin != null),
            // The official app seeds the odometer with a placeholder value until the car is built.
            OrderStep("Production terminée", null, done = odometer != null && odometer != PLACEHOLDER_ODOMETER),
            OrderStep("Documents envoyés", null, done = false),
            OrderStep("Véhicule immatriculé", formatDate(expectedReg), done = false),
            // Only a confirmed appointment is shown under the step; the delivery window lives in Order.deliverySlot.
            OrderStep("Livraison", appointment, done = appointment != null),
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
        )
    }

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
        window.split(Regex("\\s*[-–]\\s*")).joinToString("\n") { part ->
            part.split(' ').joinToString(" ") { if (it.length > 4 && it.all(Char::isLetter)) it.take(3) else it }
        }

    private const val PLACEHOLDER_ODOMETER = 30.0
}

internal fun JsonObject?.obj(key: String): JsonObject =
    (this?.get(key) as? JsonObject) ?: JsonObject(emptyMap())

internal fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

/** First non-blank scalar stored under [key] anywhere in the tree (depth-first). */
internal fun JsonElement.deepFind(key: String): String? = when (this) {
    is JsonObject -> (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: values.firstNotNullOfOrNull { it.deepFind(key) }
    is kotlinx.serialization.json.JsonArray -> firstNotNullOfOrNull { it.deepFind(key) }
    else -> null
}
