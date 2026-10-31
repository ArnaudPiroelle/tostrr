package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AuthRepository
import org.koin.core.annotation.Factory

/** Prepares a login and returns the URL of Tesla's login page. */
@Factory
class StartLoginUseCase(private val auth: AuthRepository) {
    operator fun invoke(): String = auth.startLogin()
}
