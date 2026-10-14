package com.tostrr.data

import com.tostrr.domain.Change
import com.tostrr.domain.Order
import com.tostrr.domain.OrderMapper
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File

/** Fetches orders, caches the last snapshot on disk and records what changed between two syncs. */
class OrderRepository(
    private val api: TeslaApi,
    private val auth: TeslaAuth,
    private val catalog: OptionCatalog,
    private val dir: File,
    private val notifier: OrderNotifier? = null,
    /** Called after each successful sync, e.g. to refresh the home-screen widget. */
    private val onRefreshed: () -> Unit = {},
) {
    /** Serialises manual and background refreshes so they never diff against a half-updated state. */
    private val refreshLock = Mutex()

    private val ordersFile = File(dir, "orders.json")
    private val historyFile = File(dir, "history.json")

    private val _orders = MutableStateFlow(loadCache())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<Map<String, List<Change>>> = _history.asStateFlow()

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
            recordChanges(_orders.value, fresh, now)
            _orders.value = fresh
            persist(fresh)
            onRefreshed()
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

    private fun recordChanges(old: List<Order>, new: List<Order>, now: Long) {
        val oldByRef = old.associateBy { it.reference }
        val merged = _history.value.toMutableMap()
        for (order in new) {
            val before = oldByRef[order.reference]
            // First time we see this order: every step already done counts as new.
            notifier?.notifyStepChanges(before, order)
            if (before == null) continue
            val changes = JsonDiff.diff(before.raw, order.raw).map { it.toChange(now) }
            if (changes.isNotEmpty()) merged[order.reference] = changes + merged[order.reference].orEmpty()
        }
        _history.value = merged
        runCatching { historyFile.writeText(encodeHistory(merged)) }
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
                        put("o", c.oldValue?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
                        put("n", c.newValue?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
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
                    (o["o"] as? JsonPrimitive)?.takeIf { p -> p.isString || p.content != "null" }?.content,
                    (o["n"] as? JsonPrimitive)?.takeIf { p -> p.isString || p.content != "null" }?.content,
                    structured = o["s"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            }
        }
    }.getOrDefault(emptyMap())

    private companion object {
        const val MAX_CHANGES_PER_ORDER = 500
        const val MAX_VALUE_CHARS = 120_000
    }
}
