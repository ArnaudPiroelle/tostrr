package app.tostrr.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import app.tostrr.data.R
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tostrr.domain.alert.Alert
import app.tostrr.domain.alert.OrderAlerts
import app.tostrr.domain.alert.TrackedDate
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettingsRepository
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OptionRepository
import app.tostrr.domain.order.StepKind
import org.koin.core.annotation.Single
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Posts what a sync found out: steps done or undone, order changes, delivery reminders, the daily digest and sync problems.
 * A kind the user turned off in the settings is skipped, but still counts as told (reminders, digest) so turning it back
 * on doesn't bring back old news.
 */
@Single
class OrderNotifier(
    private val context: Context,
    private val catalog: OptionRepository,
    private val settings: NotificationSettingsRepository,
) {
    private val prefs = context.getSharedPreferences("tostrr_notifications", Context.MODE_PRIVATE)

    /** True when at least one notification was posted, so the caller can skip the minor-update one. */
    fun notifyStepChanges(before: Order?, after: Order): Boolean {
        var notified = false
        val beforeDone = before?.steps.orEmpty().filter { it.done }.map { it.kind }.toSet()
        after.steps.filter { it.done && it.kind !in beforeDone }
            // Done by the delivery itself rather than an appointment: the delivered alert tells it.
            .filterNot { it.kind == StepKind.DELIVERY && after.appointment == null && after.delivered }
            .forEach { post(after, it.kind.name, string(it.kind.doneMessage)); notified = true }

        // A step that was done and no longer is: Tesla moved the order backwards. One notification per sync.
        val afterDone = after.steps.filter { it.done }.map { it.kind }.toSet()
        val undone = before?.steps.orEmpty().filter { it.done && it.kind !in afterDone }.map { it.kind }
        if (undone.isNotEmpty()) {
            val steps = undone.joinToString(", ") { string(R.string.notif_step_quoted, string(it.label)) }
            post(after, REGRESSION_KEY, plural(R.plurals.notif_regression, undone.size, steps))
            notified = true
        }

        if (before != null) {
            notified = notifyConfigurationChange(before, after) or notified
            notified = notifyDeliveryWindowChange(before, after) or notified
            OrderAlerts.changes(before, after).forEach { alert ->
                val (title, text) = words(alert)
                post(after, alert.key, title, text, channel = Channel.Alerts)
                notified = true
            }
        }
        return notified
    }

    /** Only for a sync whose JSON changed without any other notification covering it; see [OrderAlerts.minorUpdate]. */
    fun notifyMinorUpdate(order: Order, changes: List<Change>) {
        val alert = OrderAlerts.minorUpdate(changes) ?: return
        val count = plural(R.plurals.notif_minor_fields, alert.fields, alert.fields)
        val lines = alert.paths.map { string(R.string.notif_bullet, it) } +
            listOfNotNull(alert.more.takeIf { it > 0 }?.let { plural(R.plurals.notif_minor_more, it, it) })
        post(order, alert.key, string(R.string.notif_minor_title), count, (listOf(count) + lines).joinToString("\n"), channel = Channel.Minor)
    }

    /** Once per horizon and appointment: the reminder is remembered so the periodic sync doesn't repeat it. */
    fun notifyReminder(order: Order, now: Long) {
        val alert = OrderAlerts.reminder(order, now) ?: return
        val sent = prefs.getStringSet(PREF_REMINDERS, emptySet()).orEmpty()
        val id = order.reference + alert.key
        if (id in sent) return
        val (title, text) = words(alert)
        post(order, alert.key, title, text, channel = Channel.Reminders)
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
        val (title, text) = words(alert)
        notify(DIGEST_ID, Channel.Digest, title, text, text, onlyAlertOnce = false)
    }

    /** The sync has stopped working: [expired] when the session needs a new login, otherwise nothing came through for a while. */
    fun notifySyncProblem(expired: Boolean, sinceMs: Long?) {
        val (title, text) = if (expired) {
            string(R.string.notif_sync_expired) to string(R.string.notif_sync_expired_text)
        } else {
            val hours = sinceMs?.let { TimeUnit.MILLISECONDS.toHours(it) }
            string(R.string.notif_sync_stale) to
                (hours?.let { string(R.string.notif_sync_stale_since, it) } ?: string(R.string.notif_sync_stale_never))
        }
        notify(SYNC_ID, Channel.Sync, title, text, text, onlyAlertOnce = true)
    }

    fun clearSyncProblem() = NotificationManagerCompat.from(context).cancel(SYNC_ID)

    /** Only the new dates are shown. A window that disappears (replaced by an appointment) isn't a change to report. */
    private fun notifyDeliveryWindowChange(before: Order, after: Order): Boolean {
        val window = after.deliveryWindow?.takeIf { it.isNotBlank() } ?: return false
        if (window == before.deliveryWindow) return false
        post(after, DELIVERY_WINDOW_KEY, string(R.string.notif_window_changed), window)
        return true
    }

    /** Tesla can rewrite the configuration after the order is placed; list what was added and removed. */
    private fun notifyConfigurationChange(before: Order, after: Order): Boolean {
        val old = before.visibleOptionCodes.toSet()
        val new = after.visibleOptionCodes.toSet()
        val removed = (old - new).map(::label)
        val added = (new - old).map(::label)
        if (removed.isEmpty() && added.isEmpty()) return false
        val sections = listOf(string(R.string.notif_config_added) to added, string(R.string.notif_config_removed) to removed)
            .filter { it.second.isNotEmpty() }
        val expanded = sections.joinToString("\n\n") { (title, items) ->
            string(R.string.notif_config_heading, title) + "\n" + items.joinToString("\n") { string(R.string.notif_bullet, it) }
        }
        val summary = sections.joinToString(" · ") { (title, items) -> string(R.string.notif_config_count, title, items.size) }
        post(after, CONFIG_KEY, string(R.string.notif_config_title), summary, expanded)
        return true
    }

    private fun label(code: String) = catalog.codes.value[code]?.let { "${it.label} ($code)" } ?: code

    /** Title and text of an [Alert]. */
    private fun words(alert: Alert): Pair<String, String> = when (alert) {
        is Alert.AppointmentCancelled ->
            string(R.string.notif_appointment_cancelled) to string(R.string.notif_appointment_cancelled_text, alert.was)
        is Alert.AppointmentMoved -> string(R.string.notif_appointment_moved) to change(alert.from, alert.to)
        is Alert.WindowGone -> string(R.string.notif_window_gone) to string(R.string.notif_window_gone_text, alert.was)
        is Alert.VinChanged -> string(R.string.notif_vin_changed) to change(alert.from, alert.to)
        is Alert.Plate -> when (val from = alert.from) {
            null -> string(R.string.notif_plate_assigned) to alert.to
            else -> string(R.string.notif_plate_changed) to change(from, alert.to)
        }
        Alert.FinalInvoiceReady -> string(R.string.notif_final_invoice) to string(R.string.notif_final_invoice_text)
        Alert.FinalPaymentDone -> string(R.string.notif_final_payment) to string(R.string.notif_final_payment_text)
        Alert.Delivered -> string(R.string.notif_delivered) to string(R.string.notif_delivered_text)
        is Alert.DateChanged -> {
            val (set, changed) = when (alert.date) {
                TrackedDate.ETA_TO_DELIVERY_CENTER -> R.string.notif_eta_set to R.string.notif_eta_changed
                TrackedDate.REGISTRATION -> R.string.notif_registration_set to R.string.notif_registration_changed
            }
            when (val from = alert.from) {
                null -> string(set) to alert.to
                else -> string(changed) to change(from, alert.to)
            }
        }
        is Alert.Reminder -> {
            val title = if (alert.tomorrow) string(R.string.notif_reminder_tomorrow)
            else plural(R.plurals.notif_reminder_days, alert.days.toInt(), alert.days)
            val todo = listOfNotNull(
                string(R.string.notif_reminder_id),
                string(R.string.notif_reminder_insurance),
                string(R.string.notif_reminder_final_payment).takeIf { alert.finalPaymentDue },
            )
            title to string(R.string.notif_reminder_text, todo.joinToString(", "))
        }
        is Alert.Digest -> string(
            R.string.notif_digest_title,
            plural(R.plurals.notif_digest_changes, alert.changes, alert.changes),
            plural(R.plurals.notif_digest_orders, alert.orders, alert.orders),
        ) to string(R.string.notif_digest_text)
        is Alert.MinorUpdate -> string(R.string.notif_minor_title) to plural(R.plurals.notif_minor_fields, alert.fields, alert.fields)
    }

    private fun change(from: String, to: String) = string(R.string.notif_change, from, to)

    private fun string(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String = context.resources.getQuantityString(id, count, *args)

    /** Without [text], the notification names the car and the order. */
    private fun post(
        order: Order,
        key: String,
        title: String,
        text: String? = null,
        bigText: String? = text,
        channel: Channel = Channel.Progress,
    ) {
        val fallback = string(R.string.notif_order_fallback, order.modelName, order.reference)
        // One id per order and key: a repeated detection replaces rather than stacks.
        notify((order.reference + key).hashCode(), channel, title, text ?: fallback, bigText ?: fallback, onlyAlertOnce = false)
    }

    private fun notify(id: Int, channel: Channel, title: String, text: String, bigText: String, onlyAlertOnce: Boolean) {
        if (!canNotify() || !settings.settings.value.isEnabled(channel.kind)) return
        ensureChannel(channel)
        // The data layer doesn't know the app's activities: open whatever the launcher opens.
        val launch = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        val open = PendingIntent.getActivity(
            context, 0,
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(context.applicationInfo.icon)
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
            manager.createNotificationChannel(NotificationChannel(channel.id, string(channel.title), channel.importance))
        }
    }

    private enum class Channel(val id: String, @StringRes val title: Int, val importance: Int, val kind: NotificationKind) {
        Progress("order_progress", R.string.channel_progress, NotificationManager.IMPORTANCE_DEFAULT, NotificationKind.PROGRESS),
        Alerts("order_alerts", R.string.channel_alerts, NotificationManager.IMPORTANCE_DEFAULT, NotificationKind.ALERTS),
        Reminders("order_reminders", R.string.channel_reminders, NotificationManager.IMPORTANCE_DEFAULT, NotificationKind.REMINDERS),
        Minor("order_minor", R.string.channel_minor, NotificationManager.IMPORTANCE_LOW, NotificationKind.MINOR_UPDATES),
        Digest("order_digest", R.string.channel_digest, NotificationManager.IMPORTANCE_LOW, NotificationKind.DIGEST),
        Sync("order_sync", R.string.channel_sync, NotificationManager.IMPORTANCE_HIGH, NotificationKind.SYNC_PROBLEMS),
    }

    /** What a step becoming done tells, and its name in the regression notification. */
    @get:StringRes
    private val StepKind.doneMessage: Int
        get() = when (this) {
            StepKind.ORDERED -> R.string.notif_step_ordered
            StepKind.VIN_ASSIGNED -> R.string.notif_step_vin_assigned
            StepKind.BUILT -> R.string.notif_step_built
            StepKind.DOCUMENTS_SENT -> R.string.notif_step_documents_sent
            StepKind.REGISTERED -> R.string.notif_step_registered
            StepKind.DELIVERY -> R.string.notif_step_delivery
        }

    @get:StringRes
    private val StepKind.label: Int
        get() = when (this) {
            StepKind.ORDERED -> R.string.step_ordered
            StepKind.VIN_ASSIGNED -> R.string.step_vin_assigned
            StepKind.BUILT -> R.string.step_built
            StepKind.DOCUMENTS_SENT -> R.string.step_documents_sent
            StepKind.REGISTERED -> R.string.step_registered
            StepKind.DELIVERY -> R.string.step_delivery
        }

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
