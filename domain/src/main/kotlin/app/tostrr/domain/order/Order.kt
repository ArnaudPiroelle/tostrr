package app.tostrr.domain.order

import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The fixed steps of an order, in their order; the screens and notifications word them. */
enum class StepKind { ORDERED, VIN_ASSIGNED, BUILT, DOCUMENTS_SENT, REGISTERED, DELIVERY }

data class OrderStep(
    val kind: StepKind,
    val date: String?,
    val done: Boolean,
)

/** How the car is handed over. */
enum class DeliveryMethod { HOME, PICKUP }

data class Order(
    val reference: String,
    val status: String,
    val vin: String?,
    val modelName: String,
    val optionCodes: List<String>,
    val imageUrl: String?,
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
    /** ISO country code of the order, e.g. `FR`. */
    val countryCode: String? = null,
    /** Day the order was placed. */
    val bookedOn: LocalDate? = null,
    val deliveryMethod: DeliveryMethod? = null,
    val vehicleInfo: VehicleInfo = VehicleInfo(),
    val paymentInfo: PaymentInfo = PaymentInfo(),
    /** See [LocalSignals.partsCatalogReference]. */
    val partsCatalogReference: String? = null,
) {
    /** Index of the first step not completed yet (the "current" one), or the last step when all are done. */
    val currentStepIndex: Int
        get() = steps.indexOfFirst { !it.done }.let { if (it == -1) steps.lastIndex else it }

    /** Option codes as the catalogue keys them (no `$` prefix, upper case), sorted, without the model markers. */
    val visibleOptionCodes: List<String>
        get() = optionCodes.map { it.removePrefix("$").uppercase() }.filter { it !in HIDDEN_OPTION_CODES }.distinct().sorted()

    /** Neither delivered nor cancelled: the order is still on its way. */
    val awaitingDelivery: Boolean
        get() = status != STATUS_DELIVERED && status != STATUS_CANCELLED

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

/**
 * The [limit] most recently placed orders still [awaiting delivery][Order.awaitingDelivery], newest first.
 * Orders without a known booking day come last.
 */
fun List<Order>.latestAwaitingDelivery(limit: Int): List<Order> =
    filter { it.awaitingDelivery }.sortedWith(compareByDescending(nullsFirst()) { it.bookedOn }).take(limit)

private const val STATUS_DELIVERED = "DELIVERED"
private const val STATUS_CANCELLED = "CANCELLED"

/** A day-first `dd/MM/yyyy` appointment text, as the mapper writes it in French, when no instant came with it. */
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
