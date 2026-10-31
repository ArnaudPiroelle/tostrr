package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The user's orders as last synced. */
@Factory
class ObserveOrdersUseCase(private val repository: OrderRepository) {
    operator fun invoke(): StateFlow<List<Order>> = repository.orders
}
