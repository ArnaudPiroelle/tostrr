package com.tostrr.domain.auth.usecase

import com.tostrr.domain.auth.AuthRepository
import com.tostrr.domain.sync.SyncScheduler
import org.koin.core.annotation.Factory

/**
 * Finishes the login with the redirect URL Tesla sent back, then starts the background sync.
 * Throws [com.tostrr.domain.error.AuthException].
 */
@Factory
class CompleteLoginUseCase(
    private val auth: AuthRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(redirect: String) {
        auth.completeLogin(redirect)
        scheduler.schedule()
    }
}
