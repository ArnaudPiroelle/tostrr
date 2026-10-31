package app.tostrr.domain.layout.usecase

import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.DetailLayoutRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The user's arrangement of the order detail screen. */
@Factory
class ObserveDetailLayoutUseCase(private val repository: DetailLayoutRepository) {
    operator fun invoke(): StateFlow<BlockLayout> = repository.layout
}
