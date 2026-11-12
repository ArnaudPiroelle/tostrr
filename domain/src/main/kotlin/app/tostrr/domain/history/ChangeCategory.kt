package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OrderMapper

/**
 * What a change is about, from its path. [important] ones are the changes that tell where the order stands; the
 * others (insurance offers, anything unrecognised) are mostly Tesla moving its own fields around.
 */
enum class ChangeCategory(val important: Boolean = true) {
    ORDER,
    VIN,
    DELIVERY_WINDOW,
    APPOINTMENT,
    DELIVERY,
    REGISTRATION,
    AGREEMENTS,
    FINANCING,
    PAYMENT,
    TRADE_IN,
    INSURANCE(important = false),
    OTHER(important = false),
}

/** The category of this change. Paths are matched case-insensitively, the most specific rule first. */
val Change.category: ChangeCategory
    get() {
        val path = key.lowercase()
        return when {
            OrderMapper.VIN_KEYS.any { it.equals(key, ignoreCase = true) } -> ChangeCategory.VIN
            path.contains("deliverywindow") || path.contains("selfschedulingdetails") || path == "$SCHEDULING.display" -> ChangeCategory.DELIVERY_WINDOW
            path.contains("appointment") || path.contains("apptdatetime") -> ChangeCategory.APPOINTMENT
            path.startsWith(TASKS) -> TASK_CATEGORIES.entries.firstOrNull { (task, _) -> path.startsWith("$TASKS.$task") }?.value ?: ChangeCategory.OTHER
            path.startsWith("order.") -> ChangeCategory.ORDER
            else -> ChangeCategory.OTHER
        }
    }

private const val TASKS = "details.tasks"
private const val SCHEDULING = "$TASKS.scheduling"

/** The `/tasks` task each category comes from, lower case; a task not listed is [ChangeCategory.OTHER]. */
private val TASK_CATEGORIES = linkedMapOf(
    "scheduling" to ChangeCategory.DELIVERY,
    "deliveryacceptance" to ChangeCategory.DELIVERY,
    "deliverydetails" to ChangeCategory.DELIVERY,
    "registration" to ChangeCategory.REGISTRATION,
    "agreements" to ChangeCategory.AGREEMENTS,
    "financing" to ChangeCategory.FINANCING,
    "finalpayment" to ChangeCategory.PAYMENT,
    "tradein" to ChangeCategory.TRADE_IN,
    "insurance" to ChangeCategory.INSURANCE,
)
