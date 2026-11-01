package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.OrderRepository
import org.koin.core.annotation.Factory

/**
 * Pulls the orders again. Throws [app.tostrr.domain.error.AuthException] when the session is gone,
 * [app.tostrr.domain.error.ApiException] on API failures.
 */
@Factory
class RefreshOrdersUseCase(private val repository: OrderRepository) {
    suspend operator fun invoke() = repository.refresh()
}
