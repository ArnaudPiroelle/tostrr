package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.UpdateRepository
import org.koin.core.annotation.Factory

/** Stops offering this version; the next one is offered again. */
@Factory
class SkipUpdateUseCase(private val repository: UpdateRepository) {
    operator fun invoke(version: String) = repository.skip(version)
}
