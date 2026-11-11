package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/**
 * Changes one answer of a finished checklist, which stays finished. [replacedPhoto] is the photo of the answer
 * being overwritten, deleted once the new answer no longer points to it.
 */
@Factory
class UpdateChecklistAnswerUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(
        ref: String,
        questionId: String,
        status: AnswerStatus,
        comment: String,
        photoPaths: List<String>,
        previousPhotos: List<String>,
    ) {
        if (repository.updateAnswer(ref, newAnswer(questionId, status, comment, photoPaths))) {
            deleteDropped(repository, previousPhotos, photoPaths)
        }
    }
}
