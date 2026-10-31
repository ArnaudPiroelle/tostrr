package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.AuthRepository
import org.koin.core.annotation.Factory

/** Fetches the account's profile if logged in and not loaded yet; never fails. */
@Factory
class LoadTeslaAccountUseCase(
    private val auth: AuthRepository,
    private val repository: AccountRepository,
) {
    suspend operator fun invoke() {
        if (auth.isLoggedIn() && repository.account.value == null) repository.load()
    }
}
