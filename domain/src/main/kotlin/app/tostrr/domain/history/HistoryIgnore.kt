package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
        "details.tasks.tradeIn.isMatched",
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

    /**
     * The Tesla staff member handling the trade-in: a field named after the employee, or a `@tesla.com` e-mail.
     * Who handles it is not news about the order. A guess: no trade-in of ours had one.
     */
    fun isStaffDetail(path: String, value: String?): Boolean =
        path.startsWith(TRADE_IN) &&
            (STAFF_KEY.containsMatchIn(path.substringAfterLast('.')) || value?.let(STAFF_EMAIL::containsMatchIn) == true)

    /** A recorded change that would not be recorded today. */
    fun ignores(change: Change): Boolean =
        isIgnored(change.key) || isStaffDetail(change.key, change.oldValue) || isStaffDetail(change.key, change.newValue)

    private const val TRADE_IN = "details.tasks.tradeIn."
    private val STAFF_KEY = Regex("employee", RegexOption.IGNORE_CASE)
    private val STAFF_EMAIL = Regex("""@tesla\.com\b""", RegexOption.IGNORE_CASE)

    /** Copy of [element] without the ignored paths and staff details, so that a diff of two pruned trees never reports them. */
    fun prune(element: JsonElement, path: String = ""): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.entries
                .map { (key, value) -> Triple(key, if (path.isEmpty()) key else "$path.$key", value) }
                .filterNot { (_, child, value) -> isDropped(child, value) }
                .associate { (key, child, value) -> key to prune(value, child) },
        )
        is JsonArray -> JsonArray(
            element.mapIndexedNotNull { i, value -> "$path.$i".let { if (isDropped(it, value)) null else prune(value, it) } },
        )
        else -> element
    }

    private fun isDropped(path: String, value: JsonElement): Boolean =
        isIgnored(path) || isStaffDetail(path, (value as? JsonPrimitive)?.contentOrNull)
}
