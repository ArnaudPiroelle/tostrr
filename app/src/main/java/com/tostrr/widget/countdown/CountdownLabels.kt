package com.tostrr.widget.countdown

import com.tostrr.domain.order.DeliveryCountdown

/**
 * What the widget shows: the big value and the unit next to it. Without room for a lead-in, the value tells the
 * states apart: a bare count before the window or the appointment, "+N" once inside the window.
 */
internal data class CountdownLabels(val value: String, val unit: String)

internal fun countdownLabels(countdown: DeliveryCountdown): CountdownLabels = when (countdown) {
    is DeliveryCountdown.Appointment -> when {
        countdown.days > 0 -> CountdownLabels("${countdown.days}", days(countdown.days))
        countdown.days == 0L -> CountdownLabels("Jour J", "")
        else -> CountdownLabels("J+${-countdown.days}", "")
    }
    is DeliveryCountdown.BeforeWindow -> CountdownLabels("${countdown.days}", days(countdown.days))
    is DeliveryCountdown.InWindow ->
        if (countdown.days == 0L) CountdownLabels("Jour 1", "")
        else CountdownLabels("+${countdown.days}", days(countdown.days))
    is DeliveryCountdown.PastWindow -> CountdownLabels("+${countdown.days}", "j de retard")
    DeliveryCountdown.Unknown -> CountdownLabels("—", "")
}

private fun days(n: Long) = if (n > 1) "jours" else "jour"
