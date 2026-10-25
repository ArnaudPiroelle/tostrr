package app.tostrr.domain.auth.usecase

import app.tostrr.domain.auth.AuthRepository
import org.koin.core.annotation.Factory

@Factory
class IsLoggedInUseCase(private val auth: AuthRepository) {
    operator fun invoke(): Boolean = auth.isLoggedIn()
}
