package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What [OrderMapper] reads besides the steps and the appointment, covered by [OrderMapperTest]. */
class OrderMapperDetailsTest {
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private fun map(order: String, tasks: String = "{}") = OrderMapper.map(obj(order), obj("""{"tasks":$tasks}"""), 42L)

    @Test fun `an order without a reference still maps`() {
        val o = map("{}")
        assertEquals("?", o.reference)
        assertEquals("", o.status)
        assertEquals("Tesla", o.modelName)
        assertEquals(StepKind.entries, o.steps.map { it.kind })
        assertTrue(o.steps.none { it.done })
        assertEquals(42L, o.fetchedAtEpochMs)
    }

    @Test fun `the raw payloads are kept for the history`() {
        val o = map("""{"referenceNumber":"RN1"}""", """{"a":1}""")
        assertEquals(setOf("order", "details"), o.raw.keys)
        assertEquals(obj("""{"referenceNumber":"RN1"}"""), o.raw["order"])
    }

    @Test fun `the model comes from the model code, else from the option codes`() {
        assertEquals("Model 3", map("""{"modelCode":"M3"}""").modelName)
        assertEquals("Cybertruck", map("""{"modelCode":"ct"}""").modelName)
        assertEquals("Model Y", map("""{"mktOptions":"${'$'}MTY13,${'$'}PPSW"}""").modelName)
        assertEquals("Model X", map("""{"mktOptions":"MTX10"}""").modelName)
        assertEquals("Tesla", map("""{"mktOptions":"PPSW"}""").modelName)
    }

    @Test fun `the car image needs a model and options`() {
        assertNull(map("""{"modelCode":"my"}""").imageUrl)
        assertNull(map("""{"mktOptions":"PPSW"}""").imageUrl)
        val url = map("""{"modelCode":"my","mktOptions":"${'$'}MTY13,${'$'}PPSW"}""").imageUrl!!
        assertTrue(url.startsWith("https://static-assets.tesla.com/configurator/compositor?"))
        assertTrue("model=my" in url)
        assertTrue("options=%24MTY13%2C%24PPSW" in url)
    }

    @Test fun `a blank vin counts as none`() {
        val o = map("""{"vin":"  "}""")
        assertNull(o.vin)
        assertFalse(o.steps[1].done)
    }

    @Test fun `a vin marks its step done`() {
        assertTrue(map("""{"vin":"LRWYGCEK1TC000104"}""").steps.first { it.kind == StepKind.VIN_ASSIGNED }.done)
    }

    @Test fun `the placed date stands in for the booked one`() {
        val o = map("{}", """{"x":{"orderPlacedDate":"2026-03-04T10:00:00Z"}}""")
        assertEquals("04/03/26", o.steps.first().date)
        assertTrue(o.steps.first().done)
    }

    @Test fun `the expected registration is shown under its step`() {
        val o = map("{}", """{"registration":{"expectedRegDate":"2026-12-01"}}""")
        assertEquals("01/12/26", o.expectedRegistration)
        assertEquals("01/12/26", o.steps.first { it.kind == StepKind.REGISTERED }.date)
    }

    @Test fun `the delivery window display string is kept and shortened for the cards`() {
        val o = map("{}", """{"scheduling":{"deliveryWindowDisplay":" 29 Novembre - 29 Décembre "}}""")
        assertEquals("29 Novembre - 29 Décembre", o.deliveryWindow)
        assertEquals("29 Nov – 29 Déc", o.deliverySlot)
    }

    @Test fun `a confirmed appointment wins over the window on the cards`() {
        val o = map("{}", """{"scheduling":{"deliveryWindowDisplay":"29 Novembre - 29 Décembre","deliveryAppointmentDate":"30 novembre à 10:00"}}""")
        assertEquals("30 novembre à 10:00", o.deliverySlot)
    }

    @Test fun `the delivery centre and country are read from scheduling`() {
        val o = map("{}", """{"scheduling":{"deliveryAddressTitle":"Tesla Lyon","countryCode":"FR"}}""")
        assertEquals("Tesla Lyon", o.deliveryCenter)
        assertEquals("FR", o.countryCode)
        assertEquals("BE", map("""{"countryCode":"BE"}""", """{"scheduling":{"countryCode":"FR"}}""").countryCode)
    }

    @Test fun `final payment flags accept booleans and strings`() {
        assertTrue(map("{}", """{"finalPayment":{"data":{"hasFinalInvoice":true}}}""").finalInvoiceReady)
        assertTrue(map("{}", """{"finalPayment":{"data":{"hasActiveInvoice":"TRUE"}}}""").finalInvoiceReady)
        assertFalse(map("{}", """{"finalPayment":{"data":{"hasFinalInvoice":"no"}}}""").finalInvoiceReady)
        assertTrue(map("{}", """{"finalPayment":{"complete":true}}""").finalPaymentDone)
        assertFalse(map("{}").finalPaymentDone)
    }

    @Test fun `paths are matched whatever the key case`() {
        assertEquals("LRWYGCEK1TC000102", map("{}", """{"Registration":{"OrderDetails":{"VIN":"LRWYGCEK1TC000102"}}}""").vin)
    }

    @Test fun `the plate is read and marks the car registered`() {
        val o = map("{}", """{"registration":{"regData":{"reggieLicensePlate":"AB-123-CD"}}}""")
        assertEquals("AB-123-CD", o.licensePlate)
        assertTrue(o.steps.first { it.kind == StepKind.REGISTERED }.done)
    }

    @Test fun `a utc appointment gives an instant even when scheduling has the display string`() {
        val o = map(
            "{}",
            """{"scheduling":{"deliveryAppointmentDate":"30 novembre à 10:30"},"deliveryAcceptance":{"appointmentDateUtc":"2026-11-30T09:30:00Z"}}""",
        )
        assertEquals("30 novembre à 10:30", o.appointment)
        assertEquals(1_796_031_000_000L, o.appointmentAtEpochMs)
    }

    @Test fun `dates that are not iso are shown as they come`() {
        assertEquals("bientôt", OrderMapper.formatDate("bientôt"))
        assertNull(OrderMapper.formatDate(" "))
        assertNull(OrderMapper.formatDate(null))
    }
}
