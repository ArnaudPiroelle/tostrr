package app.tostrr.domain.sync

/** The periodic background sync of the orders. */
interface SyncScheduler {
    /** Idempotent: keeps the existing schedule if there is one, only moving it to [interval] if that changed. */
    fun schedule(interval: SyncInterval)

    fun cancel()
}
