package app.tostrr.domain.layout

/** A block of a screen the user can reorder or hide. */
interface LayoutBlock {
    /** Stable key saved in the layout; never rename one. */
    val id: String
}

/** The blocks of the order detail screen, below its fixed header. Declaration order is the default order. */
enum class DetailBlock(override val id: String) : LayoutBlock {
    CHECKLIST("checklist"),
    DELIVERY("delivery"),
    PROGRESS("progress"),
    CONFIGURATION("configuration"),
    HISTORY("history"),
}

/**
 * The user's arrangement of a screen: block ids in their order, and the hidden ones. Empty means the default
 * order with every block shown, so a default layout is never stored.
 */
data class BlockLayout(val order: List<String> = emptyList(), val hidden: List<String> = emptyList()) {
    val isDefault: Boolean get() = order.isEmpty() && hidden.isEmpty()
}
