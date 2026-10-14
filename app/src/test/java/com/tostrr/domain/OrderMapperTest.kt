package com.tostrr.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderMapperTest {
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private fun map(order: String, tasks: String) =
        OrderMapper.map(obj(order), obj("""{"tasks":$tasks}"""), 0L)

    @Test fun `a vin only found in the insurance task is ignored`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"insurance":{"vin":"OTHERCARVIN0000001"}}""")
        assertNull(o.vin)
    }

    @Test fun `vin from the order wins, then the registration paths`() {
        assertEquals("ORDERVIN", map("""{"referenceNumber":"RN1","vin":"ORDERVIN"}""", "{}").vin)
        assertEquals("REGVIN", map("""{"referenceNumber":"RN1"}""", """{"registration":{"orderDetails":{"vin":"REGVIN"}}}""").vin)
        assertEquals("PAYVIN", map("""{"referenceNumber":"RN1"}""", """{"finalPayment":{"data":{"vin":"PAYVIN"}}}""").vin)
    }

    @Test fun `scheduling appointment is kept as the display string`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"scheduling":{"deliveryAppointmentDate":"  29  novembre  à 10:00 "}}""")
        assertEquals("29 novembre à 10:00", o.appointment)
    }

    @Test fun `appointment falls back to the other sources`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"scheduling":{"deliveryAppointmentDate":""},"deliveryAcceptance":{"appointmentDate":"2026-11-30T10:30:00"}}""")
        assertEquals("30/11/2026 10:30", o.appointment)
        assertEquals(true, o.steps.last().done)
    }

    @Test fun `no appointment anywhere leaves the step open`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"scheduling":{"deliveryAppointmentDate":""}}""")
        assertNull(o.appointment)
        assertEquals(false, o.steps.last().done)
    }

    @Test fun `a utc appointment is shown in local time`() {
        val shown = OrderMapper.formatAppointment("2026-11-30T09:30:00Z")
        // Device time zone dependent, but always the same shape.
        assertEquals(true, Regex("30/11/2026 \\d{2}:30|01/12/2026 \\d{2}:30|30/11/2026 \\d{2}:\\d{2}").matches(shown))
    }
}
