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
import com.tostrr.domain.Order

/** Posts a notification for each order step that was completed since the previous sync. */
class OrderNotifier(private val context: Context) {

    fun notifyNewlyDoneSteps(before: Order?, after: Order) {
        val beforeDone = before?.steps.orEmpty().filter { it.done }.map { it.label }.toSet()
        after.steps.filter { it.done && it.label !in beforeDone && it.doneMessage.isNotEmpty() }
            .forEach { post(after, it.label, it.doneMessage) }
    }

    private fun post(order: Order, stepLabel: String, message: String) {
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
            .setContentText("${order.modelName} · ${order.reference}")
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
    }
}
