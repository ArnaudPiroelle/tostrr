package app.tostrr.domain.layout.usecase

import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.layout.DetailLayoutRepository
import org.koin.core.annotation.Factory

/** Moves a shown block of the order detail up ([delta] < 0) or down among the shown ones. */
@Factory
class MoveDetailBlockUseCase(private val repository: DetailLayoutRepository) {
    operator fun invoke(block: DetailBlock, delta: Int) =
        repository.save(BlockLayouts.move(DetailBlock.entries, repository.layout.value, block.id, delta))
}
