package app.tostrr.domain.layout.usecase

import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailLayoutRepository
import org.koin.core.annotation.Factory

/** Back to the default order, every block shown; collapsed blocks stay collapsed. */
@Factory
class ResetDetailLayoutUseCase(private val repository: DetailLayoutRepository) {
    operator fun invoke() = repository.save(BlockLayouts.resetArrangement(repository.layout.value))
}
