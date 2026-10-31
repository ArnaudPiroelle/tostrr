package app.tostrr.widget.countdown

import app.tostrr.domain.order.DeliveryCountdown
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CountdownLabelsTest {
    private val resources = RuntimeEnvironment.getApplication().resources

    private fun labels(countdown: DeliveryCountdown) = countdownLabels(resources, countdown)

    @Test fun `days before the appointment, singular up to one`() {
        assertEquals(CountdownLabels("12", "jours"), labels(DeliveryCountdown.Appointment(12)))
        assertEquals(CountdownLabels("1", "jour"), labels(DeliveryCountdown.Appointment(1)))
    }

    @Test fun `the day of the appointment, then the days after it`() {
        assertEquals(CountdownLabels("Jour J", ""), labels(DeliveryCountdown.Appointment(0)))
        assertEquals(CountdownLabels("J+3", ""), labels(DeliveryCountdown.Appointment(-3)))
    }

    @Test fun `inside and past the delivery window`() {
        assertEquals(CountdownLabels("5", "jours"), labels(DeliveryCountdown.BeforeWindow(5)))
        assertEquals(CountdownLabels("Jour 1", ""), labels(DeliveryCountdown.InWindow(0)))
        assertEquals(CountdownLabels("+2", "jours"), labels(DeliveryCountdown.InWindow(2)))
        assertEquals(CountdownLabels("+4", "j de retard"), labels(DeliveryCountdown.PastWindow(4)))
        assertEquals(CountdownLabels("—", ""), labels(DeliveryCountdown.Unknown))
    }
}
