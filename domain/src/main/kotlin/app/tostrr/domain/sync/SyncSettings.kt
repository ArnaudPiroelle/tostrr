package app.tostrr.domain.sync

/** How the orders are fetched: in the background every [interval] when [autoSync] is on, else only on demand. */
data class SyncSettings(
    val autoSync: Boolean = true,
    val interval: SyncInterval = SyncInterval.DEFAULT,
)
