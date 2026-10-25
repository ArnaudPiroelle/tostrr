package com.tostrr.domain.alert

import com.tostrr.domain.order.Change
import com.tostrr.domain.order.Order
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class OrderAlertsTest {
    private fun order(
        appointment: String? = null,
        window: String? = null,
        vin: String? = null,
        plate: String? = null,
        invoice: Boolean = false,
        paid: Boolean = false,
        eta: String? = null,
        at: Long? = null,
    ) = Order(
        reference = "RN1", status = "", vin = vin, modelName = "Model 3", optionCodes = emptyList(), imageUrl = null,
        bookedDate = null, steps = emptyList(), deliveryCenter = null, deliveryWindow = window, deliverySlot = null,
        appointment = appointment, etaToDeliveryCenter = eta, expectedRegistration = null, fetchedAtEpochMs = 0L,
        raw = JsonObject(emptyMap()), licensePlate = plate, appointmentAtEpochMs = at,
        finalInvoiceReady = invoice, finalPaymentDone = paid,
    )

    private fun keys(a: Order, b: Order) = OrderAlerts.changes(a, b).map { it.key }

    @Test fun `a new appointment is left to the step notification`() {
        assertEquals(emptyList<String>(), keys(order(window = "1 - 2 nov"), order(appointment = "5 nov")))
    }

    @Test fun `a moved or cancelled appointment is reported`() {
        assertEquals("12 nov → 14 nov", OrderAlerts.changes(order(appointment = "12 nov"), order(appointment = "14 nov")).single().text)
        assertEquals("Rendez-vous de livraison annulé", OrderAlerts.changes(order(appointment = "12 nov"), order()).single().title)
    }

    @Test fun `a window that vanishes alone is reported, not one replaced by an appointment`() {
        assertEquals(listOf("window_gone"), keys(order(window = "1 - 2 nov"), order()))
    }

    @Test fun `vin appearing is a step but vin changing is an alert`() {
        assertEquals(emptyList<String>(), keys(order(), order(vin = "A")))
        assertEquals(listOf("vin"), keys(order(vin = "A"), order(vin = "B")))
    }

    @Test fun `plate and payment transitions`() {
        assertEquals(listOf("plate"), keys(order(), order(plate = "AB-123-CD")))
        assertEquals(listOf("final_invoice"), keys(order(), order(invoice = true)))
        assertEquals(listOf("final_payment"), keys(order(invoice = true), order(invoice = true, paid = true)))
        assertEquals(emptyList<String>(), keys(order(invoice = true), order(invoice = true)))
    }

    @Test fun `eta change names both dates`() {
        assertEquals("01/11/26 → 05/11/26", OrderAlerts.changes(order(eta = "01/11/26"), order(eta = "05/11/26")).single().text)
    }

    @Test fun `reminders cover three days then one day, nothing outside`() {
        val day = TimeUnit.DAYS.toMillis(1)
        val now = 1_000_000_000L
        assertNull(OrderAlerts.reminder(order(at = now + 5 * day), now))
        assertNull(OrderAlerts.reminder(order(at = now - 1), now))
        assertNull(OrderAlerts.reminder(order(), now))
        val d3 = OrderAlerts.reminder(order(at = now + 2 * day + 1), now)!!
        assertEquals("Livraison dans 3 jours", d3.title)
        val d1 = OrderAlerts.reminder(order(at = now + day / 2), now)!!
        assertTrue(d1.key.endsWith("d1"))
        assertTrue(d1.text.contains("paiement final"))
        assertTrue(!OrderAlerts.reminder(order(at = now + day / 2, paid = true), now)!!.text.contains("paiement final"))
    }

    @Test fun `digest counts only recent changes`() {
        val h = mapOf("RN1" to listOf(Change(100, "a", "1", "2"), Change(10, "b", "1", "2")))
        assertEquals("1 modification sur 1 commande", OrderAlerts.digest(h, 50)!!.title)
        assertNull(OrderAlerts.digest(h, 200))
        assertNotNull(OrderAlerts.digest(h, 0))
    }
}

class DeliveryDayTest {
    private val zone = java.time.ZoneId.of("Europe/Paris")
    private fun at(s: String) = java.time.LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()
    private fun order(appointment: String? = null, at: Long? = null) = Order(
        reference = "RN1", status = "", vin = null, modelName = "Model 3", optionCodes = emptyList(), imageUrl = null,
        bookedDate = null, steps = emptyList(), deliveryCenter = null, deliveryWindow = null, deliverySlot = null,
        appointment = appointment, etaToDeliveryCenter = null, expectedRegistration = null, fetchedAtEpochMs = 0L,
        raw = JsonObject(emptyMap()), appointmentAtEpochMs = at,
    )

    @Test fun `true all day long on the appointment date, false the day before and after`() {
        val o = order(at = at("2026-11-30T10:30:00"))
        assertTrue(o.isDeliveryDay(at("2026-11-30T00:05:00"), zone))
        assertTrue(o.isDeliveryDay(at("2026-11-30T23:55:00"), zone))
        assertTrue(!o.isDeliveryDay(at("2026-11-29T23:55:00"), zone))
        assertTrue(!o.isDeliveryDay(at("2026-12-01T00:05:00"), zone))
    }

    @Test fun `falls back to the formatted appointment text`() {
        assertTrue(order(appointment = "30/11/2026 10:30").isDeliveryDay(at("2026-11-30T08:00:00"), zone))
        assertTrue(!order(appointment = "30 novembre à 10:30").isDeliveryDay(at("2026-11-30T08:00:00"), zone))
        assertTrue(!order().isDeliveryDay(at("2026-11-30T08:00:00"), zone))
    }

    @Test fun `a minor update lists the changed paths and caps the list`() {
        assertNull(OrderAlerts.minorUpdate(emptyList()))
        val one = OrderAlerts.minorUpdate(listOf(Change(0L, "details.a", "1", "2")))!!
        assertEquals("minor_update", one.key)
        assertEquals("1 champ modifié\n• details.a", one.text)
        val many = OrderAlerts.minorUpdate((1..7).map { Change(0L, "p$it", null, "x") })!!
        assertEquals(listOf("7 champs modifiés", "• p1", "• p2", "• p3", "• p4", "• p5", "… et 2 autres"), many.text.lines())
    }
}
