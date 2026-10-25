package com.tostrr.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Paths whose changes never reach the history (a guess, not documented by Tesla). Paths look like `details.tasks.x.0.y`.
 *
 * There are four lists: exact paths, `prefix*`, `*suffix` and `*contains*`.
 * They mostly hide what Tesla renders for the UI (localised card texts, `.strings`, screen descriptions),
 * internal signals and eligibility flags, so a language or wording change is not reported as an order change.
 */
object HistoryIgnore {

    val Exact: List<String> = emptyList()

    /** A pattern ending in `.` matches the subtree below that key. */
    val Prefixes: List<String> = listOf(
        "details.tasks.registration.isMatched",
        "details.tasks.registration.orderDetails.vehicleModelYear",
        "details.state.",
        "details.scheduling.card.",
        "details.tasks.carbonCredit.card.",
        "details.tasks.finalPayment.card.",
        "details.tasks.scheduling.card.",
        "details.tasks.scheduling.isDeliveryEstimatesEnabled",
        "details.tasks.registration.orderDetails.isAvailableForMatch",
        "details.tasks.finalPayment.data.isAvailableForMatch",
        "details.tasks.finalPayment.data.agreementDetails",
        "details.tasks.finalPayment.data.vehicleId",
        "details.tasks.deliveryAcceptance.gates",
        "details.tasks.deliveryAcceptance.card.",
        "details.tasks.deliveryDetails.card.",
        "details.tasks.registration.card.",
        "details.tasks.finalPayment.complete",
        "details.tasks.finalPayment.data.finalPaymentStatus",
        "details.tasks.scheduling.isInventoryOrMatched",
        "details.tasks.scheduling.isMoreThanTwoWeeks",
        "details.tasks.finalPayment.data.hasFinalInvoice",
        "details.tasks.finalPayment.data.hasActiveInvoice",
        "details.tasks.finalPayment.data.selfSchedulingDetails.deliveryLocationId",
        "details.tasks.financing.card.",
        "details.tasks.tradeIn.card.",
        "internal.signals.",
        "details.localSignals.",
    )

    val Suffixes: List<String> = listOf(".strings", ".screen.description", ".order")

    val Contains: List<String> = listOf(".strings.")

    fun isIgnored(path: String): Boolean {
        if (path in Exact) return true
        // Trailing dot so that `a.card.` also matches the container `a.card` itself.
        val dotted = "$path."
        return Prefixes.any { dotted.startsWith(it) || (!it.endsWith('.') && path.startsWith(it)) } ||
            Suffixes.any { path.endsWith(it) } ||
            Contains.any { dotted.contains(it) }
    }

    /** Copy of [element] without the ignored paths, so that a diff of two pruned trees never reports them. */
    fun prune(element: JsonElement, path: String = ""): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.entries
                .map { (key, value) -> key to (if (path.isEmpty()) key else "$path.$key") }
                .filterNot { (_, child) -> isIgnored(child) }
                .associate { (key, child) -> key to prune(element.getValue(key), child) },
        )
        is JsonArray -> JsonArray(
            element.mapIndexedNotNull { i, value -> "$path.$i".let { if (isIgnored(it)) null else prune(value, it) } },
        )
        else -> element
    }
}
