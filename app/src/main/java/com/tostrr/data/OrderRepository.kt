package com.tostrr.data

import android.content.Context
import com.tostrr.domain.Change
import com.tostrr.domain.Order
import com.tostrr.domain.OrderMapper
import com.tostrr.domain.HistoryIgnore
import com.tostrr.domain.JsonDiff
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.koin.core.annotation.Single
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

/** Fetches orders, caches the last snapshot on disk and records what changed between two syncs. */
@Single
class OrderRepository(
    context: Context,
    private val api: TeslaApi,
    private val auth: TeslaAuth,
    private val catalog: OptionCatalog,
    private val notifier: OrderNotifier,
    /** Told after each successful sync, e.g. to refresh the home-screen widgets. */
    private val refreshListener: OrderRefreshListener,
) {
    /** Serialises manual and background refreshes so they never diff against a half-updated state. */
    private val refreshLock = Mutex()

    private val dir = context.filesDir
    private val ordersFile = File(dir, "orders.json")
    private val historyFile = File(dir, "history.json")

    /**
     * Every successful pull is kept here, one gzipped JSON per order and per sync, for later analysis.
     * Readable over adb without root, so real payloads can be pulled.
     */
    private val archiveDir = context.getExternalFilesDir("snapshots") ?: File(dir, "snapshots")

    private val _orders = MutableStateFlow(loadCache())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _history = MutableStateFlow(loadHistory().let(::dropIgnored))
    val history: StateFlow<Map<String, List<Change>>> = _history.asStateFlow()

    /** Age of the last successful pull, or null if there has never been one. */
    fun lastSyncAgeMs(now: Long = System.currentTimeMillis()): Long? =
        _orders.value.maxOfOrNull { it.fetchedAtEpochMs }?.takeIf { it > 0 }?.let { now - it }

    fun order(reference: String): Order? = _orders.value.firstOrNull { it.reference == reference }

    /** Throws [AuthException] when the session is gone, [ApiException] on API failures. */
    suspend fun refresh() {
        refreshLock.withLock {
            val token = auth.validAccessToken() ?: throw AuthException("Non connecté.")
            val now = System.currentTimeMillis()
            val fresh = api.listOrders(token).map { it.jsonObject }.map { order ->
                val ref = order["referenceNumber"]?.jsonPrimitive?.content ?: error("Commande sans référence")
                OrderMapper.map(order, api.orderTasks(token, ref), now)
            }
            // The cache written by an older version has no archive yet: keep it as the first snapshot.
            archive(_orders.value)
            recordChanges(_orders.value, fresh, now)
            _orders.value = fresh
            persist(fresh)
            saveHistory(_history.value)
            archive(fresh)
            notifier.clearSyncProblem()
            fresh.forEach { notifier.notifyReminder(it, now) }
            notifier.notifyDigest(_history.value, now)
            refreshListener.onOrdersRefreshed()
        }
        // Best effort and outside the lock: a slow or unreachable catalogue must not delay or fail a sync.
        catalog.refreshIfStale()
    }

    fun clear() {
        ordersFile.delete()
        historyFile.delete()
        _orders.value = emptyList()
        _history.value = emptyMap()
    }

    /** Entries recorded before a path became ignored are purged, and the file is rewritten if any went. */
    private fun dropIgnored(history: Map<String, List<Change>>): Map<String, List<Change>> {
        val kept = history.mapValues { (_, changes) -> changes.filterNot { HistoryIgnore.isIgnored(it.key) } }
            .filterValues { it.isNotEmpty() }
        if (kept != history) runCatching { historyFile.writeText(encodeHistory(kept)) }
        return kept
    }

    private fun recordChanges(old: List<Order>, new: List<Order>, now: Long) {
        val oldByRef = old.associateBy { it.reference }
        val merged = _history.value.toMutableMap()
        for (order in new) {
            val before = oldByRef[order.reference]
            // First time we see this order: every step already done counts as new.
            val notified = notifier.notifyStepChanges(before, order)
            if (before == null) continue
            val changes = JsonDiff.diff(HistoryIgnore.prune(before.raw), HistoryIgnore.prune(order.raw)).map { it.toChange(now) }
            if (changes.isNotEmpty()) merged[order.reference] = changes + merged[order.reference].orEmpty()
            // The JSON moved but nothing above was worth a notification of its own.
            if (!notified) notifier.notifyMinorUpdate(order, changes)
        }
        _history.value = merged
    }

    /** Up to [MAX_CHANGES_PER_ORDER] entries of [MAX_VALUE_CHARS] each: too big to encode and write on the main thread. */
    private suspend fun saveHistory(history: Map<String, List<Change>>) = withContext(Dispatchers.IO) {
        runCatching { historyFile.writeText(encodeHistory(history)) }
    }

    private fun JsonDiff.Delta.toChange(now: Long) = Change(
        timestampEpochMs = now,
        key = path,
        oldValue = old?.asText(),
        newValue = new?.asText(),
        structured = old.isContainer() || new.isContainer(),
    )

    private fun JsonElement?.isContainer() = this is JsonObject || this is JsonArray

    /** Scalars as plain text; objects and arrays as compact JSON, truncated so one event can't bloat the history file. */
    private fun JsonElement.asText(): String = when (this) {
        is JsonNull -> ""
        is JsonPrimitive -> content
        else -> toString().take(MAX_VALUE_CHARS)
    }

    private suspend fun archive(orders: List<Order>) = withContext(Dispatchers.IO) {
        // Best effort: a full disk must never break a sync.
        orders.forEach { order -> runCatching { archiveOne(order) } }
    }

    private fun archiveOne(order: Order) {
        if (order.fetchedAtEpochMs <= 0) return
        val folder = File(archiveDir, order.reference).apply { mkdirs() }
        val file = File(folder, "${snapshotStamp.format(Instant.ofEpochMilli(order.fetchedAtEpochMs))}.json.gz")
        if (file.exists()) return
        val json = buildJsonObject {
            put("fetchedAt", order.fetchedAtEpochMs)
            put("raw", order.raw)
        }.toString()
        // Written under a temporary name so an interrupted write never leaves a truncated snapshot.
        val tmp = File(folder, file.name + ".tmp")
        GZIPOutputStream(tmp.outputStream()).bufferedWriter().use { it.write(json) }
        tmp.renameTo(file)
    }

    private suspend fun persist(orders: List<Order>) = withContext(Dispatchers.IO) {
        runCatching {
            val arr = buildJsonArray {
                orders.forEach { add(buildJsonObject { put("fetchedAt", it.fetchedAtEpochMs); put("raw", it.raw) }) }
            }
            ordersFile.writeText(arr.toString())
        }
    }

    private fun loadCache(): List<Order> = runCatching {
        Json.parseToJsonElement(ordersFile.readText()).jsonArray.map {
            val o = it.jsonObject
            val raw = o["raw"]!!.jsonObject
            OrderMapper.map(raw["order"]!!.jsonObject, raw["details"]!!.jsonObject, o["fetchedAt"]!!.jsonPrimitive.longOrNull ?: 0L)
        }
    }.getOrDefault(emptyList())

    private fun encodeHistory(map: Map<String, List<Change>>): String = buildJsonObject {
        map.forEach { (ref, changes) ->
            put(ref, buildJsonArray {
                changes.take(MAX_CHANGES_PER_ORDER).forEach { c ->
                    add(buildJsonObject {
                        put("t", c.timestampEpochMs)
                        put("k", c.key)
                        put("o", c.oldValue?.let(::JsonPrimitive) ?: JsonNull)
                        put("n", c.newValue?.let(::JsonPrimitive) ?: JsonNull)
                        if (c.structured) put("s", true)
                    })
                }
            })
        }
    }.toString()

    private fun loadHistory(): Map<String, List<Change>> = runCatching {
        Json.parseToJsonElement(historyFile.readText()).jsonObject.mapValues { (_, v) ->
            v.jsonArray.map {
                val o = it.jsonObject
                Change(
                    o["t"]!!.jsonPrimitive.longOrNull ?: 0L,
                    o["k"]!!.jsonPrimitive.content,
                    (o["o"] as? JsonPrimitive)?.contentOrNull,
                    (o["n"] as? JsonPrimitive)?.contentOrNull,
                    structured = o["s"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            }
        }
    }.getOrDefault(emptyMap())

    private companion object {
        val snapshotStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneId.systemDefault())
        const val MAX_CHANGES_PER_ORDER = 500
        const val MAX_VALUE_CHARS = 120_000
    }
}
