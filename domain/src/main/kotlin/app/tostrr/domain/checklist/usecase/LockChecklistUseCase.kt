package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Locks a completed checklist, so its report can't be changed after the fact. */
@Factory
class LockChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String) = repository.lock(ref)
}
