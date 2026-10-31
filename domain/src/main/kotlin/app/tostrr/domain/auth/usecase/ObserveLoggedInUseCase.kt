package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** Whether a Tesla session is open; follows logins and logouts from any screen. */
@Factory
class ObserveLoggedInUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<Boolean> = auth.loggedIn
}
