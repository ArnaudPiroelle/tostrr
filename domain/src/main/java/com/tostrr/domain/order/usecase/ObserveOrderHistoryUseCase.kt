package com.tostrr.domain.order.usecase

import com.tostrr.domain.order.Change
import com.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Changes recorded per order reference, oldest first. */
@Factory
class ObserveOrderHistoryUseCase(private val repository: OrderRepository) {
    operator fun invoke(): StateFlow<Map<String, List<Change>>> = repository.history
}
