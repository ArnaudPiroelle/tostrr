package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.sync.SyncScheduler
import org.koin.core.annotation.Factory

/** Ends the session: stops the background sync and forgets the cached orders and the account profile. */
@Factory
class LogoutUseCase(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
    private val orders: OrderRepository,
    private val account: AccountRepository,
) {
    operator fun invoke() {
        scheduler.cancel()
        auth.logout()
        orders.clear()
        account.clear()
    }
}
