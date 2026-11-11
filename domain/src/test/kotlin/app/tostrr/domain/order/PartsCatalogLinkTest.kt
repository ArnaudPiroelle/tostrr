package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class PartsCatalogLinkTest {
    private fun order(vin: String?, reference: String?) = OrderMapper.map(
        Json.parseToJsonElement(if (vin == null) """{"referenceNumber":"RN1"}""" else """{"referenceNumber":"RN1","vin":"$vin"}""").jsonObject,
        Json.parseToJsonElement("{}").jsonObject,
        0L,
        local = LocalSignals(reference?.let { java.time.LocalDate.of(2026, 11, 11) }, reference),
    )

    @Test fun `the link opens the catalogue of the car in the device's language and country`() {
        assertEquals(
            "https://parts.tesla.com/fr-FR/catalogs?catalogExternalReference=47b31c21&vin=LRWYGCEK1TC000001",
            order("LRWYGCEK1TC000001", "47b31c21").partsCatalogUrl(Locale.FRANCE),
        )
    }

    @Test fun `a language without a country gets one`() {
        assertEquals(true, order("LRWYGCEK1TC000001", "r").partsCatalogUrl(Locale.ENGLISH)!!.startsWith("https://parts.tesla.com/en-US/"))
        assertEquals(true, order("LRWYGCEK1TC000001", "r").partsCatalogUrl(Locale.FRENCH)!!.startsWith("https://parts.tesla.com/fr-FR/"))
    }

    @Test fun `no link without the catalogue's reference or a vin`() {
        assertNull(order("LRWYGCEK1TC000001", null).partsCatalogUrl(Locale.FRANCE))
        assertNull(order(null, "r").partsCatalogUrl(Locale.FRANCE))
    }

    @Test fun `the reference is kept in the raw payload and read back`() {
        val order = order("LRWYGCEK1TC000001", "r")
        val local = LocalSignals.fromJson(order.raw[LocalSignals.KEY]!!.jsonObject)
        assertEquals("r", local.partsCatalogReference)
        assertEquals("r", order.partsCatalogReference)
    }
}
