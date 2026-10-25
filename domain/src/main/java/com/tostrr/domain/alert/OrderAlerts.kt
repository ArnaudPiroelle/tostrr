package com.tostrr.domain.alert

import com.tostrr.domain.order.Change
import com.tostrr.domain.order.Order
import java.util.concurrent.TimeUnit

/** What to tell the user; [key] is stable per kind of event so a repeat replaces the earlier notification. */
data class Alert(val key: String, val title: String, val text: String)

/** Decides which notifications a pair of syncs deserves. Step changes, options and delivery window live in the notifier. */
object OrderAlerts {

    fun changes(before: Order, after: Order): List<Alert> = buildList {
        appointment(before, after)?.let(::add)
        windowGone(before, after)?.let(::add)
        vin(before, after)?.let(::add)
        plate(before, after)?.let(::add)
        payment(before, after).forEach(::add)
        date("eta", "Arrivée estimée au centre de livraison", before.etaToDeliveryCenter, after.etaToDeliveryCenter)?.let(::add)
        date("registration_date", "Date d'immatriculation prévue", before.expectedRegistration, after.expectedRegistration)?.let(::add)
    }

    /** Created appointments are reported by the step itself; only a move or a cancellation is news here. */
    private fun appointment(before: Order, after: Order): Alert? {
        val old = before.appointment ?: return null
        val new = after.appointment
        return when {
            new == old -> null
            new == null -> Alert("appointment", "Rendez-vous de livraison annulé", "Il était prévu le $old")
            else -> Alert("appointment", "Rendez-vous de livraison déplacé", "$old → $new")
        }
    }

    /** A window replaced by an appointment is the normal path (the step reports it); a window that just vanishes is not. */
    private fun windowGone(before: Order, after: Order): Alert? {
        val old = before.deliveryWindow?.takeIf { it.isNotBlank() } ?: return null
        if (!after.deliveryWindow.isNullOrBlank() || after.appointment != null) return null
        return Alert("window_gone", "La fenêtre de livraison a disparu", "Elle était : $old")
    }

    /** A VIN appearing is the "VIN attribué" step; a VIN that changes is unusual. */
    private fun vin(before: Order, after: Order): Alert? {
        val old = before.vin ?: return null
        val new = after.vin ?: return null
        return if (old == new) null else Alert("vin", "Le VIN de votre Tesla a changé", "$old → $new")
    }

    private fun plate(before: Order, after: Order): Alert? {
        val new = after.licensePlate ?: return null
        val old = before.licensePlate
        return when (old) {
            new -> null
            null -> Alert("plate", "Plaque d'immatriculation attribuée", new)
            else -> Alert("plate", "Plaque d'immatriculation modifiée", "$old → $new")
        }
    }

    private fun payment(before: Order, after: Order): List<Alert> = buildList {
        if (!before.finalInvoiceReady && after.finalInvoiceReady) {
            add(Alert("final_invoice", "Facture finale disponible", "Le paiement final peut être effectué"))
        }
        if (!before.finalPaymentDone && after.finalPaymentDone) {
            add(Alert("final_payment", "Paiement final enregistré", "Tesla a bien reçu le paiement"))
        }
    }

    private fun date(key: String, title: String, old: String?, new: String?): Alert? = when {
        new == null || new == old -> null
        old == null -> Alert(key, title, new)
        else -> Alert(key, "$title modifiée", "$old → $new")
    }

    /** A reminder is due once per horizon (3 days, then 1 day) before the appointment; [key] tells which one. */
    fun reminder(order: Order, now: Long): Alert? {
        val at = order.appointmentAtEpochMs ?: return null
        val left = at - now
        if (left <= 0 || left > 3 * DAY) return null
        val tomorrow = left <= DAY
        val title = if (tomorrow) "Livraison dans moins de 24 h" else "Livraison dans ${TimeUnit.MILLISECONDS.toDays(left + DAY - 1)} jours"
        val todo = buildList {
            add("pièce d'identité")
            add("assurance du véhicule")
            if (!order.finalPaymentDone) add("paiement final à effectuer")
        }
        return Alert("reminder_${at}_${if (tomorrow) "d1" else "d3"}", title, "À prévoir : ${todo.joinToString(", ")}")
    }

    /** One line for the changes recorded since [since]; null when there is nothing to say. */
    fun digest(history: Map<String, List<Change>>, since: Long): Alert? {
        val recent = history.mapValues { (_, changes) -> changes.count { it.timestampEpochMs > since } }.filterValues { it > 0 }
        val total = recent.values.sum()
        if (total == 0) return null
        val orders = if (recent.size == 1) "1 commande" else "${recent.size} commandes"
        return Alert("digest", "$total ${if (total == 1) "modification" else "modifications"} sur $orders", "Ouvrez Tostrr pour voir le détail dans l'historique")
    }

    /**
     * A sync whose JSON changed without anything the other alerts recognise. First line: how many fields; then up to
     * [MINOR_SHOWN] paths. The key is fixed so a later minor update replaces the previous one instead of stacking.
     */
    fun minorUpdate(changes: List<Change>): Alert? {
        if (changes.isEmpty()) return null
        val count = if (changes.size == 1) "1 champ modifié" else "${changes.size} champs modifiés"
        val paths = changes.map { it.key }.distinct()
        val lines = paths.take(MINOR_SHOWN).map { "• $it" } +
            listOfNotNull((paths.size - MINOR_SHOWN).takeIf { it > 0 }?.let { "… et $it autres" })
        return Alert("minor_update", "Mise à jour mineure de votre commande", (listOf(count) + lines).joinToString("\n"))
    }

    private const val MINOR_SHOWN = 5
    private val DAY = TimeUnit.DAYS.toMillis(1)
}
