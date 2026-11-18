package app.tostrr.data.order

import android.content.Context
import app.tostrr.data.api.TeslaApi
import app.tostrr.data.auth.TeslaAuth
import app.tostrr.data.catalog.PartsCatalog
import app.tostrr.data.notification.OrderNotifier
import app.tostrr.data.shipping.ShipTracker
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.history.HistoryIgnore
import app.tostrr.domain.history.JsonDiff
import app.tostrr.domain.history.undatedVinChange
import app.tostrr.domain.history.withVinAssignmentDate
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.LocalSignals
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderMapper
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.shipping.ShipVoyages
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.CancellationException
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

/** Fetches orders, caches the last snapshot on disk and records what changed between two syncs. */
@Single
class OrderRepositoryImpl(
    context: Context,
    private val api: TeslaApi,
    private val auth: TeslaAuth,
    private val partsCatalog: PartsCatalog,
    private val shipTracker: ShipTracker,
    private val notifier: OrderNotifier,
) : OrderRepository {
    /** Serialises manual and background refreshes so they never diff against a half-updated state. */
    private val refreshLock = Mutex()

    /**
     * Bumped by [clear], which can't wait for [refreshLock]: a pull started before a logout drops what it fetched
     * instead of bringing the logged-out account's orders back. Checked and written under [writeLock], with the files.
     */
    private var session = 0
    private val writeLock = Any()

    private val dir = context.filesDir
    private val ordersFile = File(dir, "orders.json")
    private val historyFile = File(dir, "history.json")

    /** Where versions up to 0.7.0 archived every pull, outside the app's private storage: removed on the next sync. */
    private val legacyArchiveDir = context.getExternalFilesDir(null)?.let { File(it, "snapshots") }

    private val _history = MutableStateFlow(loadHistory().let(::dropIgnored))
    override val history: StateFlow<Map<String, List<Change>>> = _history.asStateFlow()

    private val _orders = MutableStateFlow(loadCache().withVinDates())
    override val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    override fun lastSyncAgeMs(now: Long): Long? =
        _orders.value.maxOfOrNull { it.fetchedAtEpochMs }?.takeIf { it > 0 }?.let { now - it }

    /** Pulls started and not finished yet, waiting for the lock included. */
    private val pulls = AtomicInteger()
    private val _syncing = MutableStateFlow(false)
    override val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    override suspend fun refresh(manual: Boolean) {
        _syncing.value = pulls.incrementAndGet() > 0
        try {
            pull(manual)
        } finally {
            _syncing.value = pulls.decrementAndGet() > 0
        }
    }

    /**
     * Mapping, diffing and notifying are CPU work on payloads Tesla sizes, so they never run on the main thread a
     * manual sync comes from.
     */
    private suspend fun pull(manual: Boolean) = withContext(Dispatchers.Default) { sync(manual) }

    private suspend fun sync(manual: Boolean) {
        val started = synchronized(writeLock) { session }
        refreshLock.withLock {
            val token = auth.validAccessToken() ?: throw AuthException(AuthError.NOT_LOGGED_IN)
            val now = System.currentTimeMillis()
            val cached = _orders.value.associateBy { it.reference }
            // VINs as of the last sync: a VIN that just appeared is looked up on the next one.
            val awaitedVins = _orders.value.filter { it.awaitingDelivery }.mapNotNull { it.vin }
            partsCatalog.checkOne(awaitedVins)
            val accountVins = if (awaitedVins.isEmpty()) emptySet() else accountVins(token)
            val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
            val failures = mutableListOf<Exception>()
            val fresh = api.listOrders(token).map { it.jsonObject }.map { order ->
                val ref = order["referenceNumber"]?.jsonPrimitive?.content ?: error("Order without a reference")
                val details = try {
                    api.orderTasks(token, ref)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One order's tasks failing must not hold back the others: it keeps the details, and the date, of
                    // the last sync that had them. Without any, the sync fails as a whole.
                    val previous = cached[ref] ?: throw e
                    val known = previous.raw["details"] as? JsonObject ?: throw e
                    failures += e
                    return@map map(order, known, previous.fetchedAtEpochMs, previous, accountVins, today)
                }
                map(order, details, now, cached[ref], accountVins, today)
            }
            // Every order failing is an outage, not one bad order: report it rather than pass old details off as new.
            if (fresh.isNotEmpty() && failures.size == fresh.size) throw failures.first()
            synchronized(writeLock) {
                if (session != started) return
                recordChanges(_orders.value, fresh, now)
                _orders.value = fresh.withVinDates()
            }
            persist(fresh, started)
            saveHistory(_history.value, started)
            dropLegacyArchive()
            if (!current(started)) return
            notifier.clearSyncProblem()
            fresh.forEach { notifier.notifyReminder(it, now) }
            notifier.notifyDigest(_history.value, now)
        }
        // Best effort and outside the lock: a slow or unreachable site must not delay or fail a sync.
        if (current(started) && _orders.value.any { it.awaitingDelivery && ShipVoyages.tracks(it.vin) }) shipTracker.refresh(force = manual)
    }

    /**
     * The order with what the parts catalogue knows of its VIN, and the day its car reached the account: kept from
     * [previous] for the same VIN, else [today] when [accountVins] has it.
     */
    private fun map(order: JsonObject, details: JsonObject, fetchedAt: Long, previous: Order?, accountVins: Set<String>, today: LocalDate): Order {
        val mapped = OrderMapper.map(order, details, fetchedAt)
        val vin = mapped.vin
        val listing = vin?.let(partsCatalog::listing)
        val inAccount = previous?.inAccountSince?.takeIf { previous.vin == vin } ?: today.takeIf { vin != null && vin in accountVins }
        if (listing == null && inAccount == null) return mapped
        return OrderMapper.map(order, details, fetchedAt, local = LocalSignals(listing?.since, listing?.reference, inAccount))
    }

    /** Best effort: without the account's vehicles, a delivery is only known from Tesla's status. */
    private suspend fun accountVins(token: String): Set<String> = try {
        api.productVins(token)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptySet()
    }

    override fun clear() {
        synchronized(writeLock) {
            session++
            ordersFile.delete()
            historyFile.delete()
            _orders.value = emptyList()
            _history.value = emptyMap()
        }
        partsCatalog.clear()
        shipTracker.clear()
        notifier.clear()
    }

    private fun current(started: Int) = synchronized(writeLock) { session == started }

    private fun List<Order>.withVinDates() = map { it.withVinAssignmentDate(_history.value[it.reference].orEmpty()) }

    /**
     * Entries recorded before a path became ignored are purged, and the file is rewritten if any went. So are those of
     * the [LOCAL] node that 0.4.0 kept inside the raw payloads.
     */
    private fun dropIgnored(history: Map<String, List<Change>>): Map<String, List<Change>> {
        val kept = history.mapValues { (_, changes) -> changes.filterNot { HistoryIgnore.ignores(it) || it.key == LOCAL || it.key.startsWith("$LOCAL.") } }
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
            if (before != null) {
                val changes = JsonDiff.diff(HistoryIgnore.prune(before.raw), HistoryIgnore.prune(order.raw)).map { it.toChange(now) }
                if (changes.isNotEmpty()) merged[order.reference] = changes + merged[order.reference].orEmpty()
                // The JSON moved but nothing above was worth a notification of its own.
                if (!notified) notifier.notifyMinorUpdate(order, changes)
            }
            val known = merged[order.reference].orEmpty()
            // The cached order is the earliest sync known to have this VIN.
            order.undatedVinChange(known, before?.fetchedAtEpochMs?.takeIf { it > 0 } ?: now)?.let { vin ->
                merged[order.reference] = (known + vin).sortedByDescending { it.timestampEpochMs }
            }
        }
        _history.value = merged
    }

    /** Up to [MAX_CHANGES_PER_ORDER] entries of [MAX_VALUE_CHARS] each: too big to encode and write on the main thread. */
    private suspend fun saveHistory(history: Map<String, List<Change>>, started: Int) = withContext(Dispatchers.IO) {
        val text = encodeHistory(history)
        synchronized(writeLock) { if (session == started) runCatching { historyFile.writeText(text) } }
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

    /** Best effort: a failure here must never break a sync, and the next one tries again. */
    private suspend fun dropLegacyArchive() = withContext(Dispatchers.IO) {
        runCatching { legacyArchiveDir?.takeIf { it.exists() }?.deleteRecursively() }
    }

    private suspend fun persist(orders: List<Order>, started: Int) = withContext(Dispatchers.IO) {
        val text = buildJsonArray { orders.forEach { add(it.toCacheJson()) } }.toString()
        synchronized(writeLock) { if (session == started) runCatching { ordersFile.writeText(text) } }
    }

    /** Tesla's payloads under `raw`, what the app found out itself next to them under [LOCAL]. */
    private fun Order.toCacheJson() = buildJsonObject {
        put("fetchedAt", fetchedAtEpochMs)
        put("raw", raw)
        local.toJson()?.let { put(LOCAL, it) }
    }

    private fun loadCache(): List<Order> = runCatching {
        Json.parseToJsonElement(ordersFile.readText()).jsonArray.map {
            val o = it.jsonObject
            val raw = o["raw"]!!.jsonObject
            OrderMapper.map(
                raw["order"]!!.jsonObject,
                raw["details"]!!.jsonObject,
                o["fetchedAt"]!!.jsonPrimitive.longOrNull ?: 0L,
                // 0.4.0 kept them inside the raw payloads.
                local = LocalSignals.fromJson((o[LOCAL] ?: raw[LOCAL]) as? JsonObject),
            )
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
        const val MAX_CHANGES_PER_ORDER = 500
        const val MAX_VALUE_CHARS = 120_000
        const val LOCAL = "local"
    }
}
