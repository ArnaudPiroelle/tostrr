package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.UpdateCheck
import app.tostrr.domain.update.UpdateRepository
import org.koin.core.annotation.Factory

/** Asks for the latest release right away, however recent the last check, and says what came of it. Never throws. */
@Factory
class CheckForUpdateNowUseCase(private val repository: UpdateRepository) {
    suspend operator fun invoke(): UpdateCheck = repository.checkNow()
}
