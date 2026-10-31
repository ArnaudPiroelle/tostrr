package app.tostrr.widget.countdown

import android.content.res.Resources
import app.tostrr.R
import app.tostrr.domain.order.DeliveryCountdown

/**
 * What the widget shows: the big value and the unit next to it. Without room for a lead-in, the value tells the
 * states apart: a bare count before the window or the appointment, "+N" once inside the window.
 */
internal data class CountdownLabels(val value: String, val unit: String)

internal fun countdownLabels(resources: Resources, countdown: DeliveryCountdown): CountdownLabels {
    fun days(n: Long) = resources.getQuantityString(R.plurals.countdown_days, n.toInt())
    return when (countdown) {
        is DeliveryCountdown.Appointment -> when {
            countdown.days > 0 -> CountdownLabels("${countdown.days}", days(countdown.days))
            countdown.days == 0L -> CountdownLabels(resources.getString(R.string.countdown_delivery_day), "")
            else -> CountdownLabels(resources.getString(R.string.countdown_after_appointment, -countdown.days), "")
        }
        is DeliveryCountdown.BeforeWindow -> CountdownLabels("${countdown.days}", days(countdown.days))
        is DeliveryCountdown.InWindow ->
            if (countdown.days == 0L) CountdownLabels(resources.getString(R.string.countdown_window_first_day), "")
            else CountdownLabels(resources.getString(R.string.countdown_in_window, countdown.days), days(countdown.days))
        is DeliveryCountdown.PastWindow ->
            CountdownLabels(resources.getString(R.string.countdown_in_window, countdown.days), resources.getString(R.string.countdown_late))
        DeliveryCountdown.Unknown -> CountdownLabels("—", "")
    }
}
