package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The newer release, if any, even one the user skipped. */
@Factory
class ObserveAvailableUpdateUseCase(private val repository: UpdateRepository) {
    operator fun invoke(): StateFlow<AppUpdate?> = repository.update
}
