package com.tostrr.domain

import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class OrderStep(
    val label: String,
    val date: String?,
    val done: Boolean,
    /** Notification text shown when this step becomes done. */
    val doneMessage: String = "",
)

data class Order(
    val reference: String,
    val status: String,
    val vin: String?,
    val modelName: String,
    val optionCodes: List<String>,
    val imageUrl: String?,
    val bookedDate: String?,
    val steps: List<OrderStep>,
    val deliveryCenter: String?,
    val deliveryWindow: String?,
    /** Short one-line delivery slot for cards: the appointment if confirmed, else the abbreviated window. */
    val deliverySlot: String?,
    val appointment: String?,
    val etaToDeliveryCenter: String?,
    val expectedRegistration: String?,
    val fetchedAtEpochMs: Long,
    val raw: JsonObject,
    val licensePlate: String? = null,
    /** Appointment as an instant, when Tesla gives it in ISO form; the display string is not parseable. */
    val appointmentAtEpochMs: Long? = null,
    /** An invoice for the final payment exists, so there is something to pay. */
    val finalInvoiceReady: Boolean = false,
    val finalPaymentDone: Boolean = false,
) {
    /** Index of the first step not completed yet (the "current" one), or the last step when all are done. */
    val currentStepIndex: Int
        get() = steps.indexOfFirst { !it.done }.let { if (it == -1) steps.lastIndex else it }

    /** Option codes as the catalogue keys them (no `$` prefix, upper case), sorted, without the model markers. */
    val visibleOptionCodes: List<String>
        get() = optionCodes.map { it.removePrefix("$").uppercase() }.filter { it !in HIDDEN_OPTION_CODES }.distinct().sorted()

    /** Tesla has confirmed a delivery appointment (a mere delivery window doesn't count). */
    val hasAppointment: Boolean
        get() = !appointment.isNullOrBlank() || appointmentAtEpochMs != null

    /** True from midnight to midnight on the day of the delivery appointment, in [zone]. */
    fun isDeliveryDay(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        appointmentDay(zone) == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    /** Day of the delivery appointment in [zone], or null when there is none or its text can't be read. */
    fun appointmentDay(zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        appointmentAtEpochMs?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: appointment?.let { APPOINTMENT_DATE.find(it) }?.destructured
                ?.let { (d, m, y) -> runCatching { LocalDate.of(y.toInt(), m.toInt(), d.toInt()) }.getOrNull() }
}

/** The `dd/MM/yyyy` the mapper puts at the start of an appointment it had to format itself. */
private val APPOINTMENT_DATE = Regex("""^(\d{2})/(\d{2})/(\d{4})""")

data class Change(
    val timestampEpochMs: Long,
    val key: String,
    /** Scalars as text; objects/arrays as compact JSON. Null means the path didn't exist on that side. */
    val oldValue: String?,
    val newValue: String?,
    /** True when either side is an object or array, so the details can be inspected as JSON. */
    val structured: Boolean = false,
)

/** Model codes carry no useful description for the user, so they are left out of option lists. */
private val HIDDEN_OPTION_CODES = setOf("MDL3", "MDLY", "MDLX", "MDLS")
