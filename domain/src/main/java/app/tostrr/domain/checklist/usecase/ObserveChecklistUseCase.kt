package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

/** The delivery checklist of an order; null until it is started. */
@Factory
class ObserveChecklistUseCase(private val repository: ChecklistRepository) {
    operator fun invoke(ref: String): Flow<Checklist?> = repository.observe(ref)
}
