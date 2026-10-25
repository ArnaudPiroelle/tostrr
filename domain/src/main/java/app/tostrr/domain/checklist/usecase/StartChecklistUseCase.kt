package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Creates the checklist on its first screen; a checklist already started is left as it is. */
@Factory
class StartChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String) = repository.start(ref)
}
