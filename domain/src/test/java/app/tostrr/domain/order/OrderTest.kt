package app.tostrr.domain.order

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class OrderTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun order(
        steps: List<OrderStep> = emptyList(),
        status: String = "BOOKED",
        appointment: String? = null,
        appointmentAt: Long? = null,
        options: List<String> = emptyList(),
    ) = Order(
        reference = "RN1", status = status, vin = null, modelName = "Model Y", optionCodes = options, imageUrl = null,
        steps = steps, deliveryCenter = null, deliveryWindow = null, deliverySlot = null,
        appointment = appointment, etaToDeliveryCenter = null, expectedRegistration = null, fetchedAtEpochMs = 0L,
        raw = JsonObject(emptyMap()), appointmentAtEpochMs = appointmentAt,
    )

    private fun steps(vararg done: Boolean) = StepKind.entries.zip(done.toList()) { kind, d -> OrderStep(kind, null, d) }

    private fun millis(text: String) = ZonedDateTime.parse(text).toInstant().toEpochMilli()

    @Test fun `the current step is the first one not done`() {
        assertEquals(0, order(steps(false, false, false, false, false, false)).currentStepIndex)
        assertEquals(2, order(steps(true, true, false, true, false, false)).currentStepIndex)
    }

    @Test fun `with every step done the current one is the last`() {
        assertEquals(5, order(steps(true, true, true, true, true, true)).currentStepIndex)
    }

    @Test fun `delivered and cancelled orders are no longer awaited`() {
        assertTrue(order(status = "BOOKED").awaitingDelivery)
        assertFalse(order(status = "DELIVERED").awaitingDelivery)
        assertFalse(order(status = "CANCELLED").awaitingDelivery)
    }

    @Test fun `an appointment counts from its text or its instant`() {
        assertFalse(order().hasAppointment)
        assertFalse(order(appointment = "  ").hasAppointment)
        assertTrue(order(appointment = "29 novembre à 10:00").hasAppointment)
        assertTrue(order(appointmentAt = 0L).hasAppointment)
    }

    @Test fun `the appointment day comes from the instant first`() {
        val o = order(appointment = "01/01/2027 10:00", appointmentAt = millis("2026-11-30T23:30:00+01:00[Europe/Paris]"))
        assertEquals(LocalDate.of(2026, 11, 30), o.appointmentDay(paris))
    }

    @Test fun `a day-first appointment text gives its day`() {
        assertEquals(LocalDate.of(2026, 11, 30), order(appointment = "30/11/2026 10:30").appointmentDay(paris))
    }

    @Test fun `an appointment text without a date gives no day`() {
        assertNull(order(appointment = "29 novembre à 10:00").appointmentDay(paris))
        assertNull(order(appointment = "31/02/2026 10:00").appointmentDay(paris))
        assertNull(order().appointmentDay(paris))
    }

    @Test fun `delivery day runs from midnight to midnight in the given zone`() {
        val o = order(appointmentAt = millis("2026-11-30T10:00:00+01:00[Europe/Paris]"))
        assertFalse(o.isDeliveryDay(millis("2026-11-29T23:59:00+01:00[Europe/Paris]"), paris))
        assertTrue(o.isDeliveryDay(millis("2026-11-30T00:00:00+01:00[Europe/Paris]"), paris))
        assertTrue(o.isDeliveryDay(millis("2026-11-30T23:59:00+01:00[Europe/Paris]"), paris))
        assertFalse(o.isDeliveryDay(millis("2026-12-01T00:00:00+01:00[Europe/Paris]"), paris))
    }

    @Test fun `codes without a known label are made readable`() {
        assertEquals("Make your final payment", humanizeCode("MAKE_YOUR_FINAL_PAYMENT"))
        assertEquals("Already worded", humanizeCode("Already worded"))
        assertEquals("CARD PAYMENT", humanizeCode("CARD PAYMENT"))
    }
}
