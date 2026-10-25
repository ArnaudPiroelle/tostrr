package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory
import java.io.InputStream

/** Copies an image picked from the gallery into the checklist's photos and returns its path. */
@Factory
class ImportChecklistPhotoUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(ref: String, questionId: String, source: InputStream): String =
        repository.importPhoto(ref, questionId, source)
}
