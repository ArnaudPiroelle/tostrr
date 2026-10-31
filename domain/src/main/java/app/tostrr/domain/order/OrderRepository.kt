package app.tostrr.domain.order

import kotlinx.coroutines.flow.StateFlow

/** The user's orders as last synced, and what changed between syncs. */
interface OrderRepository {
    val orders: StateFlow<List<Order>>

    /** Changes recorded per order reference, oldest first. */
    val history: StateFlow<Map<String, List<Change>>>

    /** Whether a pull is running, from the app or the background sync. */
    val syncing: StateFlow<Boolean>

    /** Age of the last successful pull, or null if there has never been one. */
    fun lastSyncAgeMs(now: Long = System.currentTimeMillis()): Long?

    /** Pulls the orders again. Throws [app.tostrr.domain.error.AuthException] when the session is gone, [app.tostrr.domain.error.ApiException] on API failures. */
    suspend fun refresh()

    /** Forgets every cached order and its history. */
    fun clear()
}
