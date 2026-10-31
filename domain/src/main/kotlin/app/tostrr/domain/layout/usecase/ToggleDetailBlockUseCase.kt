package app.tostrr.domain.layout.usecase

import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.layout.DetailLayoutRepository
import org.koin.core.annotation.Factory

/** Hides a block of the order detail, or shows it again. */
@Factory
class ToggleDetailBlockUseCase(private val repository: DetailLayoutRepository) {
    operator fun invoke(block: DetailBlock) =
        repository.save(BlockLayouts.toggle(DetailBlock.entries, repository.layout.value, block.id))
}
