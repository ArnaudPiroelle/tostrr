package app.tostrr.domain.layout

/** A block of a screen the user can reorder or hide. */
interface LayoutBlock {
    /** Stable key saved in the layout; never rename one. */
    val id: String

    /** Left out until the user shows it from the arrangement sheet. */
    val hiddenByDefault: Boolean get() = false
}

/** The blocks of the order detail screen, below its fixed header. Declaration order is the default order. */
enum class DetailBlock(override val id: String, override val hiddenByDefault: Boolean = false) : LayoutBlock {
    PROGRESS_TRACKER("progressTracker"),
    CHECKLIST("checklist"),
    DELIVERY("delivery"),
    SHIPPING("shipping"),
    /** The step-by-step view; the one-line tracker above is enough by default. */
    PROGRESS("progress", hiddenByDefault = true),
    VEHICLE("vehicle"),
    PAYMENT("payment"),
    CONFIGURATION("configuration"),
    HISTORY("history"),
}

/**
 * The user's arrangement of a screen, as changes from the defaults: block ids in their order, the blocks hidden and
 * the [hidden-by-default][LayoutBlock.hiddenByDefault] ones shown, and the collapsed ones. Empty means the default
 * order, visibility and expansion, so a default layout is never stored.
 */
data class BlockLayout(
    val order: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val collapsed: List<String> = emptyList(),
    val shown: List<String> = emptyList(),
) {
    /** Order and visibility are the default; collapsing isn't part of the arrangement "reset" undoes. */
    val isArrangementDefault: Boolean get() = order.isEmpty() && hidden.isEmpty() && shown.isEmpty()

    val isDefault: Boolean get() = isArrangementDefault && collapsed.isEmpty()
}
