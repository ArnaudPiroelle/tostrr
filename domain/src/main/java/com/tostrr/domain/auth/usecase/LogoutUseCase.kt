package com.tostrr.domain.auth.usecase

import com.tostrr.domain.auth.AuthRepository
import com.tostrr.domain.order.OrderRepository
import com.tostrr.domain.sync.SyncScheduler
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
