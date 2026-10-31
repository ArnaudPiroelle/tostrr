package app.tostrr.data.api

import app.tostrr.domain.error.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.util.Locale
import java.util.UUID

/** Raw, schema-less access to the private Tesla order and account endpoints (payloads change often). */
@Single
class TeslaApi(private val client: OkHttpClient) {

    suspend fun listOrders(accessToken: String): JsonArray {
        val root = get(ORDERS_URL.toHttpUrl(), accessToken)
        return root.jsonObject["response"]?.jsonArray ?: JsonArray(emptyList())
    }

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
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", USER_AGENT)
            .header("X-Tesla-User-Agent", X_USER_AGENT)
            .header("X-Request-Id", UUID.randomUUID().toString())
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code)
            Json.parseToJsonElement(text)
        }
    }

    private companion object {
        const val ORDERS_URL = "https://owner-api.teslamotors.com/api/1/users/orders"
        const val ME_URL = "https://owner-api.teslamotors.com/api/1/users/me"
        const val TASKS_URL = "https://akamai-apigateway-vfx.tesla.com/tasks"
        const val APP_VERSION = "9.99.9-9999"
        const val USER_AGENT = "Tesla/4.55.5 (com.teslamotors.tesla; build:4193; Android 14)"
        const val X_USER_AGENT = "TeslaApp/4.55.5-4193/4193/android/14"
    }
}
