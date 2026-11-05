package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/** The newer release to bring up on its own: none if the user skipped it. */
@Factory
class ObserveUpdateToOfferUseCase(private val repository: UpdateRepository) {
    operator fun invoke(): StateFlow<AppUpdate?> = repository.updateToOffer
}
