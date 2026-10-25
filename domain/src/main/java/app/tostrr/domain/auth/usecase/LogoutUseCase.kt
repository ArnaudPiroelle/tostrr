package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.sync.SyncScheduler
import org.koin.core.annotation.Factory

/** Ends the session: stops the background sync and forgets the cached orders. */
@Factory
class LogoutUseCase(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
    private val orders: OrderRepository,
) {
    operator fun invoke() {
        scheduler.cancel()
        auth.logout()
        orders.clear()
    }
}
