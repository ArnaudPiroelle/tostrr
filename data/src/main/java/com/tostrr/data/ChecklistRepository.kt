package com.tostrr.data

import android.content.Context
import com.tostrr.domain.AnswerStatus
import com.tostrr.domain.Checklist
import com.tostrr.domain.ChecklistAnswer
import com.tostrr.domain.DeliveryChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File
import java.io.InputStream

/** Delivery checklists, one per order, and the photos taken for their answers. */
@Single
class ChecklistRepository(private val dao: ChecklistDao, context: Context) {
    private val photoRoot = File(context.filesDir, "checklist_photos")

    fun observe(ref: String): Flow<Checklist?> =
        combine(dao.observeChecklist(ref), dao.observeAnswers(ref)) { checklist, answers ->
            checklist?.let {
                Checklist(
                    orderReference = it.orderReference,
                    startedAtEpochMs = it.startedAtEpochMs,
                    completedAtEpochMs = it.completedAtEpochMs,
                    cursor = it.cursor,
                    answers = answers.associate { a -> a.questionId to a.toDomain() },
                )
            }
        }

    /** Creates the checklist on its first screen; a checklist already started is left as it is. */
    suspend fun start(ref: String, now: Long = System.currentTimeMillis()) {
        if (dao.checklist(ref) == null) dao.upsert(ChecklistEntity(ref, now, completedAtEpochMs = null, cursor = 0))
    }

    /** Moves to [cursor]; moving past the last step completes the checklist, which can't move after that. */
    suspend fun moveTo(ref: String, cursor: Int, now: Long = System.currentTimeMillis()) {
        val current = dao.checklist(ref) ?: return
        if (current.completedAtEpochMs != null) return
        dao.upsert(current.movedTo(cursor, now))
    }

    /** Reopens a completed checklist at [cursor], its answers kept, so they can be changed. */
    suspend fun reopen(ref: String, cursor: Int) {
        val current = dao.checklist(ref) ?: return
        dao.upsert(current.copy(cursor = cursor.coerceIn(0, DeliveryChecklist.steps.lastIndex), completedAtEpochMs = null))
    }

    /** Replaces one answer of a finished checklist, which stays finished. */
    suspend fun updateAnswer(ref: String, answer: ChecklistAnswer) {
        if (dao.checklist(ref)?.completedAtEpochMs == null) return
        dao.upsert(answer.toEntity(ref))
    }

    /** Deletes the checklist, its answers and its photos. */
    suspend fun delete(ref: String) {
        dao.delete(ref)
        withContext(Dispatchers.IO) { File(photoRoot, ref).deleteRecursively() }
    }

    /** Records [answer] and moves to [nextCursor] in one go. Ignored once the checklist is completed. */
    suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int) {
        val current = dao.checklist(ref) ?: return
        if (current.completedAtEpochMs != null) return
        dao.saveAnswer(answer.toEntity(ref), current.movedTo(nextCursor, answer.answeredAtEpochMs))
    }

    /** Copies a picked image into the checklist's photo folder and returns its path. */
    suspend fun importPhoto(ref: String, questionId: String, source: InputStream): String = withContext(Dispatchers.IO) {
        val file = newPhotoFile(ref, questionId)
        source.use { input -> file.outputStream().use { input.copyTo(it) } }
        file.absolutePath
    }

    /** A fresh file for a photo of [questionId]: a new name each time, so an image cache never shows a replaced shot. */
    fun newPhotoFile(ref: String, questionId: String): File =
        File(File(photoRoot, ref).apply { mkdirs() }, "$questionId-${System.currentTimeMillis()}.jpg")

    /** Removes a photo that was taken but not kept (replaced, or the answer was never recorded with it). */
    suspend fun deletePhoto(path: String) = withContext(Dispatchers.IO) {
        File(path).takeIf { it.canonicalPath.startsWith(photoRoot.canonicalPath) }?.delete()
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
    photoPath = photoPath,
    answeredAtEpochMs = answeredAtEpochMs,
)

private fun ChecklistAnswer.toEntity(ref: String) = ChecklistAnswerEntity(
    orderReference = ref,
    questionId = questionId,
    status = status.name,
    comment = comment,
    photoPath = photoPath,
    answeredAtEpochMs = answeredAtEpochMs,
)
