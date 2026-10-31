package app.tostrr.data.api

import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.error.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TeslaApiTest {
    private val http = FakeHttp()
    private val api = TeslaApi(http.client)

    @Test fun `orders are unwrapped from the response, with the token sent along`() = runTest {
        http.on("/api/1/users/orders", """{"response":[{"referenceNumber":"RN1"},{"referenceNumber":"RN2"}],"count":2}""")
        val orders = api.listOrders("token-1")
        assertEquals(listOf("RN1", "RN2"), orders.map { it.jsonObject["referenceNumber"]!!.jsonPrimitive.content })
        val request = http.calls.single().request
        assertEquals("owner-api.teslamotors.com", request.url.host)
        assertEquals("Bearer token-1", request.header("Authorization"))
        assertTrue(request.header("User-Agent")!!.startsWith("Tesla/"))
        assertTrue(request.header("X-Request-Id")!!.isNotBlank())
    }

    @Test fun `no response field means no orders`() = runTest {
        http.on("/api/1/users/orders", """{"count":0}""")
        assertTrue(api.listOrders("t").isEmpty())
    }

    @Test fun `the tasks are asked in the given language for the order`() = runTest {
        http.on("/tasks", """{"tasks":{}}""")
        api.orderTasks("t", "RN1", Locale.FRANCE)
        val url = http.calls.single().request.url
        assertEquals("fr", url.queryParameter("deviceLanguage"))
        assertEquals("FR", url.queryParameter("deviceCountry"))
        assertEquals("RN1", url.queryParameter("referenceNumber"))
        assertTrue(url.queryParameter("appVersion")!!.isNotBlank())
    }

    @Test fun `a locale without country or language falls back to English in France`() = runTest {
        http.on("/tasks", "{}")
        api.orderTasks("t", "RN1", Locale.ROOT)
        val url = http.calls.single().request.url
        assertEquals("en", url.queryParameter("deviceLanguage"))
        assertEquals("FR", url.queryParameter("deviceCountry"))
    }

    @Test fun `the profile is unwrapped, and missing when it isn't an object`() = runTest {
        http.on("/api/1/users/me", """{"response":{"full_name":"Jane Doe"}}""")
        assertEquals("Jane Doe", api.me("t")!!["full_name"]!!.jsonPrimitive.content)
        http.on("/api/1/users/me", """{"response":null}""")
        assertNull(api.me("t"))
    }

    @Test fun `an http error becomes an api exception with its code`() = runTest {
        http.on("/api/1/users/orders", "Too many requests", code = 429)
        val e = runCatching { api.listOrders("t") }.exceptionOrNull()
        assertEquals(429, (e as ApiException).code)
    }
}
