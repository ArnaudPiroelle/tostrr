package app.tostrr.domain.shipping.usecase

import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.shipping.ShipTrackingRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The latest snapshot of the tracked ships, null until one was fetched. */
@Factory
class ObserveShipSnapshotUseCase(private val repository: ShipTrackingRepository) {
    operator fun invoke(): StateFlow<ShipSnapshot?> = repository.snapshot
}
