package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.order.latestAwaitingDelivery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

/** The [limit] most recently placed orders not delivered yet, newest first, updated after every sync. */
@Factory
class ObserveLatestAwaitingDeliveryUseCase(private val repository: OrderRepository) {
    operator fun invoke(limit: Int): Flow<List<Order>> = repository.orders.map { it.latestAwaitingDelivery(limit) }
}
