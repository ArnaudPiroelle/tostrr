package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Deletes the checklist of an order, its answers and its photos. */
@Factory
class DeleteChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String) = repository.delete(ref)
}
