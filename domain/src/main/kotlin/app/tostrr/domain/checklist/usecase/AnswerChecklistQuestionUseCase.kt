package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.ChecklistRepository
import org.koin.core.annotation.Factory

/**
 * Records the answer on the screen at [cursor] and moves to the next one. [replacedPhoto] is the photo of the
 * answer being overwritten, deleted once the new answer no longer points to it.
 */
@Factory
class AnswerChecklistQuestionUseCase(private val repository: ChecklistRepository) {
    suspend operator fun invoke(
        ref: String,
        cursor: Int,
        questionId: String,
        status: AnswerStatus,
        comment: String,
        photoPaths: List<String>,
        previousPhotos: List<String>,
    ) {
        if (repository.answer(ref, newAnswer(questionId, status, comment, photoPaths), cursor + 1)) {
            deleteDropped(repository, previousPhotos, photoPaths)
        }
    }
}

internal fun newAnswer(questionId: String, status: AnswerStatus, comment: String, photoPaths: List<String>) = ChecklistAnswer(
    questionId = questionId,
    status = status,
    comment = comment.trim().ifEmpty { null },
    photoPaths = photoPaths.distinct(),
    answeredAtEpochMs = System.currentTimeMillis(),
)

/** The photos of the answer being overwritten that the new one no longer has. */
internal suspend fun deleteDropped(repository: ChecklistRepository, previous: List<String>, kept: List<String>) {
    (previous - kept.toSet()).forEach { repository.deletePhoto(it) }
}
