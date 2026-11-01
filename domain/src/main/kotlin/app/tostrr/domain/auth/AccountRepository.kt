package app.tostrr.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** The profile of the logged-in Tesla account, kept on the device until logout so it shows offline. */
interface AccountRepository {
    /** The last profile fetched; null before the first one (or if it has no name), and once logged out. */
    val account: StateFlow<TeslaAccount?>

    /** Fetches the profile again. Best effort: a failure keeps what was there and never throws. */
    suspend fun load()

    fun clear()
}
