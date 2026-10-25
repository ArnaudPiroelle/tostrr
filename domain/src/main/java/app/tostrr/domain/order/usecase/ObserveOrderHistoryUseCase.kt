package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Changes recorded per order reference, oldest first. */
@Factory
class ObserveOrderHistoryUseCase(private val repository: OrderRepository) {
    operator fun invoke(): StateFlow<Map<String, List<Change>>> = repository.history
}
