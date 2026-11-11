package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class OrderMapperTest {
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private fun map(order: String, tasks: String) =
        OrderMapper.map(obj(order), obj("""{"tasks":$tasks}"""), 0L)

    @Test fun `booking day is read from the booked date, wherever it sits`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"registration":{"orderDetails":{"orderBookedDate":"2026-07-14T09:12:00.000Z"}}}""")
        assertEquals(LocalDate.of(2026, 7, 14), o.bookedOn)
        assertNull(map("""{"referenceNumber":"RN1"}""", "{}").bookedOn)
    }

    @Test fun `a vin only found in the insurance task is ignored`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"insurance":{"vin":"OTHERCARVIN0000001"}}""")
        assertNull(o.vin)
    }

    @Test fun `vin from the order wins, then the registration paths`() {
        assertEquals("LRWYGCEK1TC000101", map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC000101"}""", "{}").vin)
        assertEquals("LRWYGCEK1TC000102", map("""{"referenceNumber":"RN1"}""", """{"registration":{"orderDetails":{"vin":"LRWYGCEK1TC000102"}}}""").vin)
        assertEquals("LRWYGCEK1TC000103", map("""{"referenceNumber":"RN1"}""", """{"finalPayment":{"data":{"vin":"LRWYGCEK1TC000103"}}}""").vin)
    }

    @Test fun `placeholders are no vin, and the next path is tried`() {
        for (placeholder in listOf("VIN ##vin##", "##VIN##", "pending", "N/A", "null", "LRW000")) {
            val o = map("""{"referenceNumber":"RN1","vin":"$placeholder"}""", "{}")
            assertNull(placeholder, o.vin)
            assertEquals(placeholder, false, o.steps.first { it.kind == StepKind.VIN_ASSIGNED }.done)
        }
        assertEquals(
            "LRWYGCEK1TC000102",
            map("""{"referenceNumber":"RN1","vin":"VIN ##vin##"}""", """{"registration":{"orderDetails":{"vin":"LRWYGCEK1TC000102"}}}""").vin,
        )
    }

    @Test fun `a vin is read out of a label, in upper case`() {
        assertEquals("LRWYGCEK1TC000101", map("""{"referenceNumber":"RN1","vin":"VIN: lrwygcek1tc000101"}""", "{}").vin)
    }

    @Test fun `seventeen characters with I, O or Q, or more than seventeen, are no vin`() {
        assertNull(map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC00000O"}""", "{}").vin)
        assertNull(map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC0000011"}""", "{}").vin)
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

    @Test fun `an appointment needs a time to count`() {
        for (shown in listOf("29 novembre à 10:00", "29 novembre à 10h30", "Nov 29, 10 AM", "29.11.2026, 10 Uhr")) {
            val o = map("""{"referenceNumber":"RN1"}""", """{"scheduling":{"deliveryAppointmentDate":"$shown"}}""")
            assertEquals(shown, o.appointment)
            assertEquals(shown, true, o.steps.last().done)
        }
    }

    @Test fun `a day without a time is no appointment but shows as the slot`() {
        val o = map(
            """{"referenceNumber":"RN1"}""",
            """{"scheduling":{"deliveryAppointmentDate":"29 novembre 2026","deliveryWindowDisplay":"29 Novembre - 29 Décembre"}}""",
        )
        assertNull(o.appointment)
        assertEquals(false, o.hasAppointment)
        assertEquals(false, o.steps.last().done)
        assertEquals("29 novembre 2026", o.deliverySlot)
    }

    @Test fun `a time-less day leaves room for a dated appointment from another source`() {
        val o = map(
            """{"referenceNumber":"RN1"}""",
            """{"scheduling":{"deliveryAppointmentDate":"29 novembre"},"deliveryAcceptance":{"appointmentDate":"2026-11-30T10:30:00"}}""",
        )
        assertEquals("30/11/2026 10:30", o.appointment)
    }

    @Test fun `an iso day without a time is shown as a date, not as an appointment`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"deliveryAcceptance":{"appointmentDate":"2026-11-30"}}""")
        assertNull(o.appointment)
        assertNull(o.appointmentAtEpochMs)
        assertEquals(OrderMapper.formatDate("2026-11-30"), o.deliverySlot)
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

    @Test fun `an unparseable appointment is kept as is and has no instant`() {
        assertEquals("bientôt", OrderMapper.formatAppointment("bientôt"))
        assertNull(OrderMapper.appointmentInstant("bientôt"))
    }

    @Test fun `a utc appointment gives its exact instant`() {
        assertEquals(1_796_031_000_000L, OrderMapper.appointmentInstant("2026-11-30T09:30:00Z"))
    }

    @Test fun `visible option codes are normalised and leave out the model markers`() {
        val o = map("""{"referenceNumber":"RN1","mktOptions":"${'$'}W38A, MDLY,pbsb,${'$'}W38A,APBS"}""", "{}")
        assertEquals(listOf("APBS", "PBSB", "W38A"), o.visibleOptionCodes)
    }
}
