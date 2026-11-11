package app.tostrr.domain.alert

import app.tostrr.domain.order.Change
import app.tostrr.domain.order.Order
import java.util.concurrent.TimeUnit

/**
 * Something worth a notification, with what it needs to be worded; the notifier writes the text. [key] is stable per
 * kind of event so a repeat replaces the earlier notification.
 */
sealed interface Alert {
    val key: String

    data class AppointmentCancelled(val was: String) : Alert {
        override val key get() = "appointment"
    }

    data class AppointmentMoved(val from: String, val to: String) : Alert {
        override val key get() = "appointment"
    }

    data class WindowGone(val was: String) : Alert {
        override val key get() = "window_gone"
    }

    data class VinChanged(val from: String, val to: String) : Alert {
        override val key get() = "vin"
    }

    /** [from] is null when the car had no plate yet. */
    data class Plate(val from: String?, val to: String) : Alert {
        override val key get() = "plate"
    }

    data object FinalInvoiceReady : Alert {
        override val key get() = "final_invoice"
    }

    data object FinalPaymentDone : Alert {
        override val key get() = "final_payment"
    }

    /** Tesla marked the order delivered, or its car reached the account. */
    data object Delivered : Alert {
        override val key get() = "delivered"
    }

    /** A date Tesla announces: set when [from] is null, else moved. */
    data class DateChanged(val date: TrackedDate, val from: String?, val to: String) : Alert {
        override val key get() = date.key
    }

    /** [days] before the appointment at [appointmentAt]; [tomorrow] when less than 24 h are left. */
    data class Reminder(val appointmentAt: Long, val days: Long, val tomorrow: Boolean, val finalPaymentDue: Boolean) : Alert {
        override val key get() = "reminder_${appointmentAt}_${if (tomorrow) "d1" else "d3"}"
    }

    data class Digest(val changes: Int, val orders: Int) : Alert {
        override val key get() = "digest"
    }

    /** [paths] are the first changed fields, [more] how many others there are. */
    data class MinorUpdate(val fields: Int, val paths: List<String>, val more: Int) : Alert {
        override val key get() = "minor_update"
    }
}

enum class TrackedDate(val key: String) {
    ETA_TO_DELIVERY_CENTER("eta"),
    REGISTRATION("registration_date"),
}

/** Decides which notifications a pair of syncs deserves. Step changes, options and delivery window live in the notifier. */
object OrderAlerts {

    fun changes(before: Order, after: Order): List<Alert> = buildList {
        appointment(before, after)?.let(::add)
        windowGone(before, after)?.let(::add)
        vin(before, after)?.let(::add)
        plate(before, after)?.let(::add)
        payment(before, after).forEach(::add)
        if (!before.delivered && after.delivered) add(Alert.Delivered)
        date(TrackedDate.ETA_TO_DELIVERY_CENTER, before.etaToDeliveryCenter, after.etaToDeliveryCenter)?.let(::add)
        date(TrackedDate.REGISTRATION, before.expectedRegistration, after.expectedRegistration)?.let(::add)
    }

    /** Created appointments are reported by the step itself; only a move or a cancellation is news here. */
    private fun appointment(before: Order, after: Order): Alert? {
        val old = before.appointment ?: return null
        val new = after.appointment
        return when {
            new == old -> null
            new == null -> Alert.AppointmentCancelled(old)
            else -> Alert.AppointmentMoved(old, new)
        }
    }

    /** A window replaced by an appointment is the normal path (the step reports it); a window that just vanishes is not. */
    private fun windowGone(before: Order, after: Order): Alert? {
        val old = before.deliveryWindow?.takeIf { it.isNotBlank() } ?: return null
        if (!after.deliveryWindow.isNullOrBlank() || after.appointment != null) return null
        return Alert.WindowGone(old)
    }

    /** A VIN appearing is the "VIN assigned" step; a VIN that changes is unusual. */
    private fun vin(before: Order, after: Order): Alert? {
        val old = before.vin ?: return null
        val new = after.vin ?: return null
        return if (old == new) null else Alert.VinChanged(old, new)
    }

    private fun plate(before: Order, after: Order): Alert? {
        val new = after.licensePlate ?: return null
        return if (before.licensePlate == new) null else Alert.Plate(before.licensePlate, new)
    }

    private fun payment(before: Order, after: Order): List<Alert> = buildList {
        if (!before.finalInvoiceReady && after.finalInvoiceReady) add(Alert.FinalInvoiceReady)
        if (!before.finalPaymentDone && after.finalPaymentDone) add(Alert.FinalPaymentDone)
    }

    private fun date(date: TrackedDate, old: String?, new: String?): Alert? =
        if (new == null || new == old) null else Alert.DateChanged(date, old, new)

    /** A reminder is due once per horizon (3 days, then 1 day) before the appointment; [Alert.key] tells which one. */
    fun reminder(order: Order, now: Long): Alert.Reminder? {
        val at = order.appointmentAtEpochMs ?: return null
        val left = at - now
        if (left <= 0 || left > 3 * DAY) return null
        return Alert.Reminder(
            appointmentAt = at,
            days = TimeUnit.MILLISECONDS.toDays(left + DAY - 1),
            tomorrow = left <= DAY,
            finalPaymentDue = !order.finalPaymentDone,
        )
    }

    /** The changes recorded since [since]; null when there is nothing to say. */
    fun digest(history: Map<String, List<Change>>, since: Long): Alert.Digest? {
        val recent = history.mapValues { (_, changes) -> changes.count { it.timestampEpochMs > since } }.filterValues { it > 0 }
        val total = recent.values.sum()
        return if (total == 0) null else Alert.Digest(total, recent.size)
    }

    /**
     * A sync whose JSON changed without anything the other alerts recognise: how many fields, and up to [MINOR_SHOWN]
     * paths. The key is fixed so a later minor update replaces the previous one instead of stacking.
     */
    fun minorUpdate(changes: List<Change>): Alert.MinorUpdate? {
        if (changes.isEmpty()) return null
        val paths = changes.map { it.key }.distinct()
        return Alert.MinorUpdate(changes.size, paths.take(MINOR_SHOWN), (paths.size - MINOR_SHOWN).coerceAtLeast(0))
    }

    private const val MINOR_SHOWN = 5
    private val DAY = TimeUnit.DAYS.toMillis(1)
}
