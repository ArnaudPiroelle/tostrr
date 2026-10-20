package com.tostrr.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tostrr.MainActivity
import com.tostrr.R
import com.tostrr.domain.Alert
import com.tostrr.domain.Change
import com.tostrr.domain.HIDDEN_OPTION_CODES
import com.tostrr.domain.Order
import com.tostrr.domain.OrderAlerts
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Posts notifications for order steps completed, or taken back, since the previous sync. */
class OrderNotifier(
    private val context: Context,
    private val catalogCodes: () -> Map<String, OptionInfo>,
) {

    fun notifyStepChanges(before: Order?, after: Order) {
        val beforeDone = before?.steps.orEmpty().filter { it.done }.map { it.label }.toSet()
        after.steps.filter { it.done && it.label !in beforeDone && it.doneMessage.isNotEmpty() }
            .forEach { post(after, it.label, it.doneMessage) }

        // A step that was done and no longer is: Tesla moved the order backwards. One notification per sync.
        val afterDone = after.steps.filter { it.done }.map { it.label }.toSet()
        val undone = before?.steps.orEmpty().filter { it.done && it.label !in afterDone }.map { it.label }
        if (undone.isNotEmpty()) {
            val steps = undone.joinToString(", ") { "« $it »" }
            val message = if (undone.size == 1) "Votre commande est revenue en arrière : l'étape $steps n'est plus validée"
            else "Votre commande est revenue en arrière : les étapes $steps ne sont plus validées"
            post(after, REGRESSION_KEY, message)
        }

        if (before != null) {
            notifyConfigurationChange(before, after)
            notifyDeliveryWindowChange(before, after)
            OrderAlerts.changes(before, after).forEach { post(after, it.key, it.title, it.text, channel = Channel.Alerts) }
        }
    }

    /** Once per horizon and appointment: the reminder is remembered so the 15-minute sync doesn't repeat it. */
    fun notifyReminder(order: Order, now: Long) {
        val alert = OrderAlerts.reminder(order, now) ?: return
        val sent = prefs.getStringSet(PREF_REMINDERS, emptySet()).orEmpty()
        val id = order.reference + alert.key
        if (id in sent) return
        post(order, alert.key, alert.title, alert.text, channel = Channel.Reminders)
        prefs.edit().putStringSet(PREF_REMINDERS, sent + id).apply()
    }

    /** At most one per day, after 9:00, and never for the first run (nothing to compare with yet). */
    fun notifyDigest(history: Map<String, List<Change>>, now: Long) {
        val last = prefs.getLong(PREF_DIGEST, 0L)
        if (last == 0L) {
            prefs.edit().putLong(PREF_DIGEST, now).apply()
            return
        }
        if (now - last < TimeUnit.HOURS.toMillis(20)) return
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        if (hour < 9) return
        prefs.edit().putLong(PREF_DIGEST, now).apply()
        val alert = OrderAlerts.digest(history, last) ?: return
        postPlain(DIGEST_ID, alert, Channel.Digest)
    }

    /** The sync has stopped working: [expired] when the session needs a new login, otherwise nothing came through for a while. */
    fun notifySyncProblem(expired: Boolean, sinceMs: Long?) {
        val alert = if (expired) {
            Alert("sync", "Connexion à Tesla expirée", "Ouvrez Tostrr et reconnectez-vous pour reprendre le suivi")
        } else {
            val hours = sinceMs?.let { TimeUnit.MILLISECONDS.toHours(it) }
            Alert("sync", "Tostrr ne reçoit plus de mises à jour", hours?.let { "Dernière synchronisation il y a $it h" } ?: "Aucune synchronisation n'a abouti")
        }
        postPlain(SYNC_ID, alert, Channel.Sync, onlyAlertOnce = true)
    }

    fun clearSyncProblem() = NotificationManagerCompat.from(context).cancel(SYNC_ID)

    /** Only the new dates are shown. A window that disappears (replaced by an appointment) isn't a change to report. */
    private fun notifyDeliveryWindowChange(before: Order, after: Order) {
        val window = after.deliveryWindow?.takeIf { it.isNotBlank() } ?: return
        if (window == before.deliveryWindow) return
        post(after, DELIVERY_WINDOW_KEY, "Votre fenêtre de livraison a changé", window)
    }

    /** Tesla can rewrite the configuration after the order is placed; list what was added and removed. */
    private fun notifyConfigurationChange(before: Order, after: Order) {
        val old = before.optionCodes.filter { it !in HIDDEN_OPTION_CODES }.toSet()
        val new = after.optionCodes.filter { it !in HIDDEN_OPTION_CODES }.toSet()
        val removed = (old - new).map(::label)
        val added = (new - old).map(::label)
        if (removed.isEmpty() && added.isEmpty()) return
        val sections = listOf("Ajouté" to added, "Retiré" to removed).filter { it.second.isNotEmpty() }
        val expanded = sections.joinToString("\n\n") { (title, items) ->
            "$title :\n" + items.joinToString("\n") { "• $it" }
        }
        val summary = sections.joinToString(" · ") { (title, items) -> "$title : ${items.size}" }
        post(after, CONFIG_KEY, "La configuration de votre Tesla évolue", summary, expanded)
    }

    private fun label(code: String) = catalogCodes()[code.uppercase()]?.let { "${it.label} ($code)" } ?: code

    private fun post(
        order: Order,
        stepLabel: String,
        message: String,
        text: String? = null,
        bigText: String? = text,
        channel: Channel = Channel.Progress,
    ) {
        // One id per order and step: a repeated detection replaces rather than stacks.
        notify((order.reference + stepLabel).hashCode(), channel, message, text ?: "${order.modelName} · ${order.reference}", bigText ?: "${order.modelName} · ${order.reference}", false)
    }

    private fun postPlain(id: Int, alert: Alert, channel: Channel, onlyAlertOnce: Boolean = false) =
        notify(id, channel, alert.title, alert.text, alert.text, onlyAlertOnce)

    private fun notify(id: Int, channel: Channel, title: String, text: String, bigText: String, onlyAlertOnce: Boolean) {
        if (!canNotify()) return
        ensureChannel(channel)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(onlyAlertOnce)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(channel: Channel) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channel.id) == null) {
            manager.createNotificationChannel(NotificationChannel(channel.id, channel.title, channel.importance))
        }
    }

    private enum class Channel(val id: String, val title: String, val importance: Int) {
        Progress("order_progress", "Avancement de la commande", NotificationManager.IMPORTANCE_DEFAULT),
        Alerts("order_alerts", "Changements de la commande", NotificationManager.IMPORTANCE_DEFAULT),
        Reminders("order_reminders", "Rappels de livraison", NotificationManager.IMPORTANCE_DEFAULT),
        Digest("order_digest", "Résumé quotidien", NotificationManager.IMPORTANCE_LOW),
        Sync("order_sync", "Problèmes de synchronisation", NotificationManager.IMPORTANCE_HIGH),
    }

    private val prefs = context.getSharedPreferences("tostrr_notifications", Context.MODE_PRIVATE)

    private companion object {
        const val PREF_REMINDERS = "reminders_sent"
        const val PREF_DIGEST = "last_digest"
        const val SYNC_ID = 0x5C01
        const val DIGEST_ID = 0x5C02
        const val REGRESSION_KEY = "regression"
        const val CONFIG_KEY = "configuration"
        const val DELIVERY_WINDOW_KEY = "delivery_window"
    }
}
