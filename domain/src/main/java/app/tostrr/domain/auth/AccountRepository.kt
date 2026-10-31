package app.tostrr.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** The profile of the logged-in Tesla account. Kept in memory only, never on disk. */
interface AccountRepository {
    /** Null until loaded, when it couldn't be (or has no name), or once logged out. */
    val account: StateFlow<TeslaAccount?>

    /** Best effort: a failure keeps what was there and never throws. */
    suspend fun load()

    fun clear()
}
