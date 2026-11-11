package app.tostrr.data.shipping

import android.content.Context
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.shipping.ShipTrackingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File

/**
 * The ships carrying Teslas to Europe, from the community site tff-ship-tracking.pages.dev: it publishes a snapshot
 * about once a day, with each ship's position, route, ETA and the VIN blocks it is believed to carry. Its index is read
 * at most every [RECHECK_MS], the snapshot only when a newer one is out; the last one is kept in `filesDir`. Only
 * public files are read: nothing about the user is sent.
 */
@Single
class ShipTracker(
    private val client: OkHttpClient,
    context: Context,
) : ShipTrackingRepository {
    private val cacheFile = File(context.filesDir, "ship_tracking.json")
    private var checkedAtMs = 0L
    private var snapshotId: String? = null

    private val _snapshot = MutableStateFlow<ShipSnapshot?>(null)
    override val snapshot: StateFlow<ShipSnapshot?> = _snapshot.asStateFlow()

    init {
        loadCache()
    }

    /** Fetches the newest snapshot if it is time to look and one came out. Never throws: this is best effort. */
    suspend fun refreshIfStale(now: Long = System.currentTimeMillis()) {
        if (now - checkedAtMs < RECHECK_MS) return
        try {
            withContext(Dispatchers.IO) {
                val id = get("$BASE_URL/snapshots/index.json")?.let(ShipSnapshotParser::latestId) ?: return@withContext
                if (id != snapshotId || _snapshot.value == null) {
                    val raw = get("$BASE_URL/snapshots/$id.json") ?: return@withContext
                    val parsed = ShipSnapshotParser.snapshot(raw) ?: return@withContext
                    _snapshot.value = parsed
                    snapshotId = id
                    checkedAtMs = now
                    save(raw)
                } else {
                    checkedAtMs = now
                    save(null)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or site unreachable: keep the last snapshot, try again next sync.
        }
    }

    fun clear() {
        _snapshot.value = null
        checkedAtMs = 0L
        snapshotId = null
        cacheFile.delete()
    }

    private fun get(url: String): JsonObject? =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            Json.parseToJsonElement(response.body?.string().orEmpty()) as? JsonObject
        }

    /** Writes the cache; a null [raw] keeps the snapshot already in it. */
    private fun save(raw: JsonObject?) {
        val snapshot = raw ?: runCatching { Json.parseToJsonElement(cacheFile.readText()).jsonObject["snapshot"] as? JsonObject }.getOrNull()
        runCatching {
            cacheFile.writeText(
                buildJsonObject {
                    put("checkedAt", checkedAtMs)
                    snapshotId?.let { put("id", it) }
                    snapshot?.let { put("snapshot", it) }
                }.toString(),
            )
        }
    }

    private fun loadCache() {
        val root = runCatching { Json.parseToJsonElement(cacheFile.readText()).jsonObject }.getOrNull() ?: return
        checkedAtMs = (root["checkedAt"] as? JsonPrimitive)?.longOrNull ?: 0L
        snapshotId = (root["id"] as? JsonPrimitive)?.contentOrNull
        _snapshot.value = (root["snapshot"] as? JsonObject)?.let(ShipSnapshotParser::snapshot)
    }

    private companion object {
        const val BASE_URL = "https://tff-ship-tracking.pages.dev"
        const val RECHECK_MS = 3 * 60 * 60 * 1000L
    }
}
