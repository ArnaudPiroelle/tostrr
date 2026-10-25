package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.sync.SyncScheduler
import org.koin.core.annotation.Factory

/**
 * Finishes the login with the redirect URL Tesla sent back, then starts the background sync.
 * Throws [app.tostrr.domain.error.AuthException].
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
