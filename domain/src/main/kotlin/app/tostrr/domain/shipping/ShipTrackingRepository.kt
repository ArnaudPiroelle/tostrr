package app.tostrr.domain.shipping

import kotlinx.coroutines.flow.StateFlow

/** The latest snapshot of the ships carrying Teslas to Europe, null until one was fetched. */
interface ShipTrackingRepository {
    val snapshot: StateFlow<ShipSnapshot?>
}
