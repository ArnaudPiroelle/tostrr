package app.tostrr.domain.order

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class OrderExportTest {
    private val order = OrderMapper.map(
        Json.parseToJsonElement("""{"referenceNumber":"RN1","orderStatus":"BOOKED","vin":"LRW3E7EK0PC000001"}""").jsonObject,
        Json.parseToJsonElement("""{"tasks":{"scheduling":{"deliveryWindowDisplay":"1 - 15 Déc"}}}""").jsonObject,
        fetchedAtEpochMs = 1_700_000_000_000,
        local = LocalSignals(partsCatalogSince = LocalDate.of(2026, 11, 11)),
    )

    private fun export(history: List<Change> = emptyList()) =
        Json.parseToJsonElement(OrderExport.json(order, history, exportedAtEpochMs = 1_800_000_000_000)).jsonObject

    @Test fun `the export holds Tesla's payloads as they came, the app's own findings apart`() {
        val json = export()
        assertEquals(OrderExport.FORMAT, json["format"]!!.jsonPrimitive.content)
        assertEquals("1", json["version"]!!.jsonPrimitive.content)
        assertEquals("2027-01-15T08:00:00Z", json["exportedAt"]!!.jsonPrimitive.content)
        assertEquals("2023-11-14T22:13:20Z", json["fetchedAt"]!!.jsonPrimitive.content)
        assertEquals("RN1", json["reference"]!!.jsonPrimitive.content)
        assertEquals("LRW3E7EK0PC000001", json["vin"]!!.jsonPrimitive.content)
        assertEquals(order.raw, json["tesla"])
        assertEquals("2026-11-11", json["local"]!!.jsonObject["partsCatalogSince"]!!.jsonPrimitive.content)
    }

    @Test fun `the history comes oldest first, structured values kept as JSON`() {
        val history = export(
            listOf(
                Change(2_000, "details.tasks.scheduling", null, """{"a":1}""", structured = true),
                Change(1_000, "order.orderStatus", "BOOKED", "IN_PRODUCTION"),
            ),
        )["history"] as JsonArray

        val first = history[0].jsonObject
        assertEquals("1970-01-01T00:00:01Z", first["at"]!!.jsonPrimitive.content)
        assertEquals("order.orderStatus", first["path"]!!.jsonPrimitive.content)
        assertEquals("BOOKED", first["old"]!!.jsonPrimitive.content)
        assertEquals("IN_PRODUCTION", first["new"]!!.jsonPrimitive.content)

        val second = history[1].jsonObject
        assertEquals(JsonNull, second["old"])
        assertEquals(JsonObject(mapOf("a" to Json.parseToJsonElement("1"))), second["new"])
    }

    @Test fun `an order the app found nothing about has no local findings`() {
        val bare = OrderMapper.map(Json.parseToJsonElement("""{"referenceNumber":"RN2"}""").jsonObject, JsonObject(emptyMap()), 0L)
        val json = Json.parseToJsonElement(OrderExport.json(bare, emptyList(), 0L)).jsonObject
        assertEquals(JsonNull, json["local"])
        assertEquals(JsonNull, json["vin"])
        assertFalse(json.toString().contains("token", ignoreCase = true))
    }
}
