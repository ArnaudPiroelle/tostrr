package app.tostrr.data.checklist

import android.content.Context
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.ChecklistRepository
import app.tostrr.domain.checklist.DeliveryChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.InputStream

/** Delivery checklists, one per order, and the photos taken for their answers. */
@Single
class ChecklistRepositoryImpl(private val dao: ChecklistDao, context: Context) : ChecklistRepository {
    private val photoRoot = File(context.filesDir, "checklist_photos")

    override fun observe(ref: String): Flow<Checklist?> =
        combine(dao.observeChecklist(ref), dao.observeAnswers(ref)) { checklist, answers ->
            checklist?.let {
                Checklist(
                    orderReference = it.orderReference,
                    startedAtEpochMs = it.startedAtEpochMs,
                    completedAtEpochMs = it.completedAtEpochMs,
                    cursor = it.cursor,
                    answers = answers.associate { a -> a.questionId to a.toDomain() },
                    lockedAtEpochMs = it.lockedAtEpochMs,
                )
            }
        }

    /** Creates the checklist on its first screen; a checklist already started is left as it is. */
    override suspend fun start(ref: String, now: Long) {
        if (dao.checklist(ref) == null) dao.upsert(ChecklistEntity(ref, now, completedAtEpochMs = null, cursor = 0))
    }

    /** Moves to [cursor]; moving past the last step completes the checklist, which can't move after that. */
    override suspend fun moveTo(ref: String, cursor: Int, now: Long) {
        val current = dao.checklist(ref) ?: return
        if (current.completedAtEpochMs != null) return
        dao.upsert(current.movedTo(cursor, now))
    }

    /** Locks a completed checklist; a locked one keeps its first lock date. */
    override suspend fun lock(ref: String, now: Long) {
        val current = dao.checklist(ref) ?: return
        if (current.completedAtEpochMs == null || current.lockedAtEpochMs != null) return
        dao.upsert(current.copy(lockedAtEpochMs = now))
    }

    /** Reopens a completed checklist at [cursor], its answers kept, so they can be changed. Ignored once locked. */
    override suspend fun reopen(ref: String, cursor: Int) {
        val current = dao.checklist(ref)?.takeIf { it.lockedAtEpochMs == null } ?: return
        dao.upsert(current.copy(cursor = cursor.coerceIn(0, DeliveryChecklist.steps.lastIndex), completedAtEpochMs = null))
    }

    /** Replaces one answer of a finished checklist, which stays finished. Ignored when not finished, or locked. */
    override suspend fun updateAnswer(ref: String, answer: ChecklistAnswer): Boolean {
        val current = dao.checklist(ref) ?: return false
        if (current.completedAtEpochMs == null || current.lockedAtEpochMs != null) return false
        dao.upsert(answer.toEntity(ref))
        return true
    }

    /** Deletes the checklist, its answers and its photos. */
    override suspend fun delete(ref: String) {
        dao.delete(ref)
        withContext(Dispatchers.IO) { File(photoRoot, ref).deleteRecursively() }
    }

    /** Records [answer] and moves to [nextCursor] in one go. Ignored once the checklist is completed. */
    override suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int): Boolean {
        val current = dao.checklist(ref) ?: return false
        if (current.completedAtEpochMs != null) return false
        dao.saveAnswer(answer.toEntity(ref), current.movedTo(nextCursor, answer.answeredAtEpochMs))
        return true
    }

    /** Copies a picked image into the checklist's photo folder and returns its path. */
    override suspend fun importPhoto(ref: String, questionId: String, source: InputStream): String = withContext(Dispatchers.IO) {
        val file = newPhotoFile(ref, questionId)
        source.use { input -> file.outputStream().use { input.copyTo(it) } }
        file.absolutePath
    }

    /** A fresh file for a photo of [questionId]: a new name each time, so an image cache never shows a replaced shot. */
    override fun newPhotoFile(ref: String, questionId: String): File =
        File(File(photoRoot, ref).apply { mkdirs() }, "$questionId-${System.currentTimeMillis()}.jpg")

    /** Removes a photo that was taken but not kept (removed, or the answer was never recorded with it). */
    override suspend fun deletePhoto(path: String) {
        withContext(Dispatchers.IO) { File(path).takeIf { it.canonicalPath.startsWith(photoRoot.canonicalPath) }?.delete() }
    }

    private fun ChecklistEntity.movedTo(cursor: Int, now: Long): ChecklistEntity {
        val last = DeliveryChecklist.steps.size
        val clamped = cursor.coerceIn(0, last)
        return copy(cursor = clamped, completedAtEpochMs = if (clamped == last) now else null)
    }
}

private fun ChecklistAnswerEntity.toDomain() = ChecklistAnswer(
    questionId = questionId,
    status = AnswerStatus.valueOf(status),
    comment = comment,
    photoPaths = photoPaths.split('\n').filter { it.isNotEmpty() },
    answeredAtEpochMs = answeredAtEpochMs,
)

private fun ChecklistAnswer.toEntity(ref: String) = ChecklistAnswerEntity(
    orderReference = ref,
    questionId = questionId,
    status = status.name,
    comment = comment,
    photoPaths = photoPaths.joinToString("\n"),
    answeredAtEpochMs = answeredAtEpochMs,
)
