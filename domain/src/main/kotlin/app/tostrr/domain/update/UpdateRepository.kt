package app.tostrr.domain.update

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The app's own releases, published on GitHub. */
interface UpdateRepository {
    /** The newer release, skipped or not: null when up to date or not known yet. */
    val update: StateFlow<AppUpdate?>

    /** [update] unless the user skipped it: the one to bring up on its own. */
    val updateToOffer: StateFlow<AppUpdate?>

    /** Asks for the latest release, if not done recently. Best effort: a failure keeps what was known and never throws. */
    suspend fun check()

    /** Stops offering [version], which stays in [update]; a later one is offered again. */
    fun skip(version: String)

    /** Fetches the APK, or finds the one already there. The flow fails if it can't be fetched. */
    fun download(update: AppUpdate): Flow<UpdateDownload>
}
