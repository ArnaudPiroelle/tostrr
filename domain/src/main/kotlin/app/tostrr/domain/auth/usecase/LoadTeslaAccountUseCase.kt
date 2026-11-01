package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.AuthRepository
import org.koin.core.annotation.Factory

/** Refreshes the account's profile if logged in; the saved one shows meanwhile. Never fails. */
@Factory
class LoadTeslaAccountUseCase(
    private val auth: AuthRepository,
    private val repository: AccountRepository,
) {
    suspend operator fun invoke() {
        if (auth.isLoggedIn()) repository.load()
    }
}
