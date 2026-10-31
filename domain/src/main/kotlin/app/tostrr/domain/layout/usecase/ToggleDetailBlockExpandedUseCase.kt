package app.tostrr.domain.layout.usecase

import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.layout.DetailLayoutRepository
import org.koin.core.annotation.Factory

/** Collapses a section of the order detail, or expands it again; remembered for every order. */
@Factory
class ToggleDetailBlockExpandedUseCase(private val repository: DetailLayoutRepository) {
    operator fun invoke(block: DetailBlock) =
        repository.save(BlockLayouts.toggleExpanded(DetailBlock.entries, repository.layout.value, block.id))
}
