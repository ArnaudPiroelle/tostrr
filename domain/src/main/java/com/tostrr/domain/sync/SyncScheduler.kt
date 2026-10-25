package com.tostrr.domain.sync

/** The periodic background sync of the orders. */
interface SyncScheduler {
    /** Idempotent: keeps the existing schedule if there is one. */
    fun schedule()

    fun cancel()
}
