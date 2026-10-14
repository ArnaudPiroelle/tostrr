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
import com.tostrr.domain.HIDDEN_OPTION_CODES
import com.tostrr.domain.Order

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

        if (before != null) notifyConfigurationChange(before, after)
    }

    /** Tesla can rewrite the configuration after the order is placed; list what was added and removed. */
    private fun notifyConfigurationChange(before: Order, after: Order) {
        val old = before.optionCodes.filter { it !in HIDDEN_OPTION_CODES }.toSet()
        val new = after.optionCodes.filter { it !in HIDDEN_OPTION_CODES }.toSet()
        val removed = (old - new).map(::label)
        val added = (new - old).map(::label)
        if (removed.isEmpty() && added.isEmpty()) return
        val lines = removed.map { "− $it" } + added.map { "+ $it" }
        post(after, CONFIG_KEY, "La configuration de votre Tesla évolue", lines.joinToString(" · "), lines.joinToString("\n"))
    }

    private fun label(code: String) = catalogCodes()[code.uppercase()]?.let { "${it.label} ($code)" } ?: code

    private fun post(order: Order, stepLabel: String, message: String, text: String? = null, bigText: String? = text) {
        if (!canNotify()) return
        ensureChannel()
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(message)
            .setContentText(text ?: "${order.modelName} · ${order.reference}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText ?: "${order.modelName} · ${order.reference}"))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One id per order and step: a repeated detection replaces rather than stacks.
        val id = (order.reference + stepLabel).hashCode()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Avancement de la commande", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "order_progress"
        const val REGRESSION_KEY = "regression"
        const val CONFIG_KEY = "configuration"
    }
}
