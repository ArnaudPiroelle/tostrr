package com.tostrr.domain.order.usecase

import com.tostrr.domain.order.OrderRepository
import org.koin.core.annotation.Factory

/**
 * Pulls the orders again. Throws [com.tostrr.domain.error.AuthException] when the session is gone,
 * [com.tostrr.domain.error.ApiException] on API failures.
 */
@Factory
class RefreshOrdersUseCase(private val repository: OrderRepository) {
    suspend operator fun invoke() = repository.refresh()
}
