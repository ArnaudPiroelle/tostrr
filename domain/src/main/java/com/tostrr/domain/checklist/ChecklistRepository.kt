package com.tostrr.domain.checklist

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

/** Delivery checklists, one per order, and the photos taken for their answers. */
interface ChecklistRepository {
    fun observe(ref: String): Flow<Checklist?>

    /** Creates the checklist on its first screen; a checklist already started is left as it is. */
    suspend fun start(ref: String, now: Long = System.currentTimeMillis())

    /** Moves to [cursor]; moving past the last step completes the checklist, which can't move after that. */
    suspend fun moveTo(ref: String, cursor: Int, now: Long = System.currentTimeMillis())

    /** Makes a finished checklist editable again, at [cursor]. */
    suspend fun reopen(ref: String, cursor: Int)

    /** Replaces one answer of a finished checklist, which stays finished. */
    suspend fun updateAnswer(ref: String, answer: ChecklistAnswer)

    /** Records [answer] and moves to [nextCursor] in one go. Ignored once the checklist is completed. */
    suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int)

    /** Deletes the checklist, its answers and its photos. */
    suspend fun delete(ref: String)

    /** Copies [source] into a new photo file and returns its path. */
    suspend fun importPhoto(ref: String, questionId: String, source: InputStream): String

    /** A fresh file for a photo of [questionId]: a new name each time, so an image cache never shows a replaced shot. */
    fun newPhotoFile(ref: String, questionId: String): File

    /** Removes a photo that was taken but not kept (replaced, or the answer was never recorded with it). */
    suspend fun deletePhoto(path: String)
}
