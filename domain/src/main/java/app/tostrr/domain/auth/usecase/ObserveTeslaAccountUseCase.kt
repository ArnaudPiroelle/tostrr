package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.TeslaAccount
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The logged-in Tesla account's name and picture, once loaded. */
@Factory
class ObserveTeslaAccountUseCase(private val repository: AccountRepository) {
    operator fun invoke(): StateFlow<TeslaAccount?> = repository.account
}
