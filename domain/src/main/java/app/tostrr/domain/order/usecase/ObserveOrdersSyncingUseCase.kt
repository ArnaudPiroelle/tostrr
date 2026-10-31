package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Whether the orders are being fetched, whoever started it (refresh button, first sync after login, background). */
@Factory
class ObserveOrdersSyncingUseCase(private val repository: OrderRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.syncing
}
