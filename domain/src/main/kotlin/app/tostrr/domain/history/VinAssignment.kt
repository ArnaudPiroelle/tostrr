package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import app.tostrr.domain.order.LocalDateFormats
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderMapper
import app.tostrr.domain.order.StepKind
import app.tostrr.domain.order.stringAt
import java.time.Instant
import java.time.ZoneId

/** Dates the VIN step with the first sync that saw this VIN, from the order's history: Tesla's payloads carry no such date. */
fun Order.withVinAssignmentDate(changes: List<Change>, zone: ZoneId = ZoneId.systemDefault()): Order {
    val vin = vin ?: return this
    val assignedAt = changes.filter { it.addsVin(vin) }.minOfOrNull { it.timestampEpochMs } ?: return this
    val date = Instant.ofEpochMilli(assignedAt).atZone(zone).toLocalDate().format(LocalDateFormats.date())
    return copy(steps = steps.map { if (it.kind == StepKind.VIN_ASSIGNED) it.copy(date = date) else it })
}

/**
 * The entry to add when the order has a VIN its history doesn't date, because it was already there on the order's first
 * sync: [syncedAt] then counts as the day it changed, and stays so on the next syncs.
 */
fun Order.undatedVinChange(changes: List<Change>, syncedAt: Long): Change? {
    val vin = vin ?: return null
    if (changes.any { it.addsVin(vin) }) return null
    val key = OrderMapper.VIN_KEYS.firstOrNull { OrderMapper.vinIn(raw.stringAt(it)) == vin } ?: return null
    return Change(syncedAt, key, null, vin)
}

/** The change is on a key the VIN is read from, or on a parent that came whole with the VIN inside. */
private fun Change.addsVin(vin: String): Boolean =
    newValue?.contains(vin) == true &&
        OrderMapper.VIN_KEYS.any { it.equals(key, ignoreCase = true) || it.startsWith("$key.", ignoreCase = true) }
