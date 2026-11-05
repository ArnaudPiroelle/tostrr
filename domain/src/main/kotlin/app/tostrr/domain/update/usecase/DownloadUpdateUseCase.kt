package app.tostrr.domain.update.usecase

import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.domain.update.UpdateRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

/** Fetches the update's APK; the flow fails if it can't be fetched. */
@Factory
class DownloadUpdateUseCase(private val repository: UpdateRepository) {
    operator fun invoke(update: AppUpdate): Flow<UpdateDownload> = repository.download(update)
}
