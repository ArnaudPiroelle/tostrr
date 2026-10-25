package com.tostrr.domain

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DeliveryCountdownTest {
    private val zone = ZoneId.of("Europe/Paris")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    private fun order(window: String? = null, appointment: String? = null, appointmentAt: Long? = null) = Order(
        reference = "RN1", status = "", vin = null, modelName = "Model Y", optionCodes = emptyList(), imageUrl = null,
        bookedDate = null, steps = emptyList(), deliveryCenter = null, deliveryWindow = window, deliverySlot = null,
        appointment = appointment, etaToDeliveryCenter = null, expectedRegistration = null, fetchedAtEpochMs = 0L,
        raw = JsonObject(emptyMap()), appointmentAtEpochMs = appointmentAt,
    )

    private val window = "29 Novembre - 29 Décembre"

    @Test fun `counts the days left before the window opens`() {
        assertEquals(DeliveryCountdown.BeforeWindow(36), order(window).deliveryCountdown(at("2026-10-24T23:30:00"), zone))
        assertEquals(DeliveryCountdown.BeforeWindow(1), order(window).deliveryCountdown(at("2026-11-28T08:00:00"), zone))
    }

    @Test fun `counts the days spent inside the window, then past it`() {
        assertEquals(DeliveryCountdown.InWindow(0), order(window).deliveryCountdown(at("2026-11-29T08:00:00"), zone))
        assertEquals(DeliveryCountdown.InWindow(30), order(window).deliveryCountdown(at("2026-12-29T22:00:00"), zone))
        assertEquals(DeliveryCountdown.PastWindow(3), order(window).deliveryCountdown(at("2027-01-01T08:00:00"), zone))
    }

    @Test fun `an appointment wins over the window`() {
        val o = order(window, appointmentAt = at("2026-12-05T10:30:00"))
        assertEquals(DeliveryCountdown.Appointment(42), o.deliveryCountdown(at("2026-10-24T12:00:00"), zone))
        assertEquals(DeliveryCountdown.Appointment(0), o.deliveryCountdown(at("2026-12-05T18:00:00"), zone))
        assertEquals(DeliveryCountdown.Appointment(-2), o.deliveryCountdown(at("2026-12-07T08:00:00"), zone))
        assertEquals(DeliveryCountdown.Appointment(1), order(appointment = "06/12/2026 09:00").deliveryCountdown(at("2026-12-05T08:00:00"), zone))
    }

    @Test fun `nothing to count without a readable window or appointment`() {
        assertEquals(DeliveryCountdown.Unknown, order().deliveryCountdown(at("2026-10-24T12:00:00"), zone))
        assertEquals(DeliveryCountdown.Unknown, order("Bientôt").deliveryCountdown(at("2026-10-24T12:00:00"), zone))
    }

    @Test fun `window years are those closest to today, the end following the start`() {
        val today = LocalDate.of(2026, 12, 15)
        assertEquals(LocalDate.of(2026, 12, 20) to LocalDate.of(2027, 1, 10), DeliveryWindow.parse("20 Décembre - 10 Janvier", today))
        assertEquals(LocalDate.of(2027, 1, 5) to LocalDate.of(2027, 2, 5), DeliveryWindow.parse("5 janv. – 5 févr.", today))
    }

    @Test fun `reads abbreviated, English and month-only windows`() {
        val today = LocalDate.of(2026, 10, 24)
        assertEquals(LocalDate.of(2026, 11, 29) to LocalDate.of(2026, 12, 29), DeliveryWindow.parse("29 Nov – 29 Déc", today))
        assertEquals(LocalDate.of(2026, 11, 29) to LocalDate.of(2026, 12, 29), DeliveryWindow.parse("November 29 - December 29", today))
        assertEquals(LocalDate.of(2026, 7, 1) to LocalDate.of(2026, 8, 31), DeliveryWindow.parse("Juillet - Août", LocalDate.of(2026, 6, 1)))
        assertEquals(LocalDate.of(2026, 6, 2) to LocalDate.of(2026, 7, 3), DeliveryWindow.parse("2 juin - 3 juil", LocalDate.of(2026, 6, 1)))
        assertNull(DeliveryWindow.parse("29 Novembre", today))
    }
}
