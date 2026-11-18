package app.tostrr.data.api

import app.tostrr.domain.error.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.IOException
import java.util.Locale
import java.util.UUID

/** Raw, schema-less access to the private Tesla order and account endpoints (payloads change often). */
@Single
class TeslaApi(private val client: OkHttpClient) {

    suspend fun listOrders(accessToken: String): JsonArray {
        val root = get(ORDERS_URL.toHttpUrl(), accessToken)
        return root.jsonObject["response"]?.jsonArray ?: JsonArray(emptyList())
    }

    /** VINs of the vehicles in the Tesla account, upper case; energy products have none. */
    suspend fun productVins(accessToken: String): Set<String> =
        (get(PRODUCTS_URL.toHttpUrl(), accessToken).jsonObject["response"] as? JsonArray).orEmpty()
            .mapNotNull { ((it as? JsonObject)?.get("vin") as? JsonPrimitive)?.contentOrNull?.trim()?.uppercase()?.takeIf(String::isNotEmpty) }
            .toSet()

    /** The account profile (`full_name`, `email`, ...), unwrapped from `response`. */
    suspend fun me(accessToken: String): JsonObject? =
        get(ME_URL.toHttpUrl(), accessToken).jsonObject["response"] as? JsonObject

    suspend fun orderTasks(accessToken: String, referenceNumber: String, locale: Locale = Locale.getDefault()): JsonObject {
        val url = TASKS_URL.toHttpUrl().newBuilder()
            .addQueryParameter("deviceLanguage", locale.language.ifEmpty { "en" })
            .addQueryParameter("deviceCountry", locale.country.ifEmpty { "FR" })
            .addQueryParameter("referenceNumber", referenceNumber)
            .addQueryParameter("appVersion", APP_VERSION)
            .build()
        return get(url, accessToken).jsonObject
    }

    private suspend fun get(url: okhttp3.HttpUrl, accessToken: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            // The account's orders and profile never go to the shared HTTP cache, whatever Tesla's headers say.
            .cacheControl(CacheControl.Builder().noStore().build())
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", USER_AGENT)
            .header("X-Tesla-User-Agent", X_USER_AGENT)
            .header("X-Request-Id", UUID.randomUUID().toString())
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code)
            Json.parseToJsonElement(text).also { if (depthOf(it) > MAX_DEPTH) throw IOException("Payload nested deeper than $MAX_DEPTH") }
        }
    }

    /**
     * Tesla's payloads are a dozen levels deep at most. The history's diff and pruning, and the mapper's searches, walk
     * them recursively: refused here, a deeper one never reaches them. Counted without recursion, for the same reason.
     */
    private fun depthOf(root: JsonElement): Int {
        var deepest = 0
        val pending = ArrayDeque<Pair<JsonElement, Int>>().apply { add(root to 1) }
        while (pending.isNotEmpty()) {
            val (element, depth) = pending.removeLast()
            deepest = maxOf(deepest, depth)
            if (deepest > MAX_DEPTH) break
            when (element) {
                is JsonObject -> element.values.forEach { pending.add(it to depth + 1) }
                is JsonArray -> element.forEach { pending.add(it to depth + 1) }
                else -> Unit
            }
        }
        return deepest
    }

    private companion object {
        const val ORDERS_URL = "https://owner-api.teslamotors.com/api/1/users/orders"
        const val ME_URL = "https://owner-api.teslamotors.com/api/1/users/me"
        const val PRODUCTS_URL = "https://owner-api.teslamotors.com/api/1/products"
        const val TASKS_URL = "https://akamai-apigateway-vfx.tesla.com/tasks"
        const val MAX_DEPTH = 64
        const val APP_VERSION = "9.99.9-9999"
        const val USER_AGENT = "Tesla/4.55.5 (com.teslamotors.tesla; build:4193; Android 14)"
        const val X_USER_AGENT = "TeslaApp/4.55.5-4193/4193/android/14"
    }
}
