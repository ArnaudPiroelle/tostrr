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
        photoPath: String?,
        replacedPhoto: String?,
    ) {
        repository.answer(ref, newAnswer(questionId, status, comment, photoPath), cursor + 1)
        if (replacedPhoto != null && replacedPhoto != photoPath) repository.deletePhoto(replacedPhoto)
    }
}

internal fun newAnswer(questionId: String, status: AnswerStatus, comment: String, photoPath: String?) = ChecklistAnswer(
    questionId = questionId,
    status = status,
    comment = comment.trim().ifEmpty { null },
    photoPath = photoPath,
    answeredAtEpochMs = System.currentTimeMillis(),
)
