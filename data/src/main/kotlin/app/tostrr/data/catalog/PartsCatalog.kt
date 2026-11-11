package app.tostrr.data.catalog

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Whether Tesla's public parts catalogue (EPC, no token needed) knows a VIN. It only lists cars that exist, so a
 * catalogue found means the car is built. Each VIN is looked up at most every [RECHECK_MS] until found, and never again
 * after; the answers are kept in `filesDir`.
 */
@Single
class PartsCatalog(
    private val client: OkHttpClient,
    context: Context,
) {
    private data class Entry(val since: LocalDate?, val checkedAtMs: Long)

    private val cacheFile = File(context.filesDir, "parts_catalog.json")
    private val entries: MutableMap<String, Entry> = load()

    /** Day the catalogue was first found for [vin], or null while it isn't. */
    fun since(vin: String): LocalDate? = entries[vin]?.since

    /**
     * Looks up the one VIN of [vins] that waited longest, if one is due: a single call per sync, so a list of orders
     * never floods the catalogue. Never throws: a failed lookup is tried again next time.
     */
    suspend fun checkOne(vins: Collection<String>, now: Long = System.currentTimeMillis()) {
        val vin = vins.distinct()
            .filter { entries[it]?.since == null && now - (entries[it]?.checkedAtMs ?: 0L) >= RECHECK_MS }
            .minByOrNull { entries[it]?.checkedAtMs ?: 0L } ?: return
        val found = try {
            withContext(Dispatchers.IO) { lookup(vin) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return
        entries[vin] = Entry(if (found) Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate() else null, now)
        withContext(Dispatchers.IO) { save() }
    }

    fun clear() {
        entries.clear()
        cacheFile.delete()
    }

    /** True when the catalogue lists [vin], false when it says it doesn't, null for any other answer. */
    private fun lookup(vin: String): Boolean? {
        val url = CATALOG_URL.toHttpUrl().newBuilder().addQueryParameter("vin", vin).build()
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            // "Not available" comes with HTTP 400, so the body decides, not the status.
            val root = runCatching { Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject }.getOrNull() ?: return null
            if ((root["success"] as? JsonPrimitive)?.booleanOrNull == true) {
                return (root["responseObject"] as? JsonArray)?.isNotEmpty()
            }
            val message = (root["message"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            return if (NOT_LISTED.any { message.contains(it, ignoreCase = true) }) false else null
        }
    }

    private fun save() {
        val json = buildJsonObject {
            entries.forEach { (vin, entry) ->
                put(vin, buildJsonObject {
                    put("c", entry.checkedAtMs)
                    entry.since?.let { put("s", it.toString()) }
                })
            }
        }
        runCatching { cacheFile.writeText(json.toString()) }
    }

    private fun load(): MutableMap<String, Entry> = runCatching {
        Json.parseToJsonElement(cacheFile.readText()).jsonObject.mapValues { (_, value) ->
            val o = value.jsonObject
            Entry(
                since = (o["s"] as? JsonPrimitive)?.contentOrNull?.let(LocalDate::parse),
                checkedAtMs = o["c"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.toMutableMap()
    }.getOrDefault(mutableMapOf())

    private companion object {
        const val CATALOG_URL = "https://epcapi.tesla.com/api/catalogs"
        const val RECHECK_MS = 60 * 60 * 1000L
        val NOT_LISTED = listOf("not available", "invalid vin")
    }
}
