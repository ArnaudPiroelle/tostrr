package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Makes a finished checklist editable again, at [cursor]. */
@Factory
class ReopenChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String, cursor: Int) = repository.reopen(ref, cursor)
}
