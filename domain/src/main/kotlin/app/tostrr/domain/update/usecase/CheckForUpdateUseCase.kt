package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.UpdateRepository
import org.koin.core.annotation.Factory

/** Looks for a newer release, if not done recently. Never fails. */
@Factory
class CheckForUpdateUseCase(private val repository: UpdateRepository) {
    suspend operator fun invoke() = repository.check()
}
