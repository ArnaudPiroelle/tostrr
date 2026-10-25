package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/** Moves to the screen at [cursor]; moving past the last step completes the checklist. */
@Factory
class MoveChecklistUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String, cursor: Int) = repository.moveTo(ref, cursor)
}
