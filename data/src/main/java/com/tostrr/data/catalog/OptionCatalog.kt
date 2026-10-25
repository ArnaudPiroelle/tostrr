package com.tostrr.data.catalog

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File

data class OptionInfo(val label: String, val category: String?)

/**
 * Option code -> description (e.g. `W38A` -> `18" Photon Wheels`).
 *
 * Tesla doesn't expose these labels. They come from the community catalogue maintained by the
 * tesla-order-status-tracker project, also used by github.com/chrisi51/tesla-order-status. It is
 * downloaded at most once per [TTL_MS] and cached; without it, screens fall back to the bare codes.
 */
@Single
class OptionCatalog(
    private val client: OkHttpClient,
    context: Context,
) {
    private val cacheFile = File(context.filesDir, "option_codes.json")
    private var fetchedAtMs = 0L

    private val _codes = MutableStateFlow<Map<String, OptionInfo>>(emptyMap())
    val codes: StateFlow<Map<String, OptionInfo>> = _codes.asStateFlow()

    init {
        loadCache()
    }

    /** Downloads the catalogue if the cache is missing or stale. Never throws: this is best effort. */
    suspend fun refreshIfStale() {
        if (_codes.value.isNotEmpty() && System.currentTimeMillis() - fetchedAtMs < TTL_MS) return
        try {
            val fresh = withContext(Dispatchers.IO) { download() } ?: return
            if (fresh.isEmpty()) return
            _codes.value = fresh
            fetchedAtMs = System.currentTimeMillis()
            withContext(Dispatchers.IO) { writeCache(fresh) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or catalogue unreachable: keep whatever is cached.
        }
    }

    private fun download(): Map<String, OptionInfo>? {
        val request = Request.Builder().url(CATALOG_URL).header("User-Agent", "Tostrr").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val root = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            if (root["ok"]?.jsonPrimitive?.booleanOrNull != true) return null
            val out = HashMap<String, OptionInfo>()
            for (entry in root["option_codes"]?.jsonArray.orEmpty()) {
                val o = entry as? JsonObject ?: continue
                val code = o.string("code")?.trim()?.uppercase() ?: continue
                val label = o.string("label_en") ?: continue
                out[code] = OptionInfo(label, o.string("category")?.trim()?.lowercase())
            }
            return out
        }
    }

    private fun writeCache(codes: Map<String, OptionInfo>) {
        val json = buildJsonObject {
            put("fetchedAt", System.currentTimeMillis())
            put("codes", buildJsonObject {
                codes.forEach { (code, info) ->
                    put(code, buildJsonObject {
                        put("l", info.label)
                        info.category?.let { put("c", it) }
                    })
                }
            })
        }
        runCatching { cacheFile.writeText(json.toString()) }
    }

    private fun loadCache() {
        runCatching {
            val root = Json.parseToJsonElement(cacheFile.readText()).jsonObject
            fetchedAtMs = root["fetchedAt"]?.jsonPrimitive?.longOrNull ?: 0L
            _codes.value = root["codes"]!!.jsonObject.mapValues { (_, v) ->
                val o = v.jsonObject
                OptionInfo(o["l"]!!.jsonPrimitive.content, o.string("c"))
            }
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private companion object {
        const val CATALOG_URL = "https://www.tesla-order-status-tracker.de/get/option_codes.php"
        const val TTL_MS = 24 * 60 * 60 * 1000L
    }
}
