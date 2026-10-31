package app.tostrr.domain.checklist

import app.tostrr.domain.checklist.usecase.AnswerChecklistQuestionUseCase
import app.tostrr.domain.checklist.usecase.UpdateChecklistAnswerUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream

class ChecklistUseCasesTest {
    /** Keeps what was answered and which photos were deleted. */
    private class FakeChecklists : ChecklistRepository {
        val answered = mutableListOf<Pair<ChecklistAnswer, Int>>()
        val updated = mutableListOf<ChecklistAnswer>()
        val deletedPhotos = mutableListOf<String>()

        override fun observe(ref: String): Flow<Checklist?> = flowOf(null)
        override suspend fun start(ref: String, now: Long) = Unit
        override suspend fun moveTo(ref: String, cursor: Int, now: Long) = Unit
        override suspend fun reopen(ref: String, cursor: Int) = Unit
        override suspend fun updateAnswer(ref: String, answer: ChecklistAnswer) {
            updated += answer
        }
        override suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int) {
            answered += answer to nextCursor
        }
        override suspend fun delete(ref: String) = Unit
        override suspend fun importPhoto(ref: String, questionId: String, source: InputStream) = ""
        override fun newPhotoFile(ref: String, questionId: String) = File(questionId)
        override suspend fun deletePhoto(path: String) {
            deletedPhotos += path
        }
    }

    private val repository = FakeChecklists()
    private val answer = AnswerChecklistQuestionUseCase(repository)
    private val update = UpdateChecklistAnswerUseCase(repository)

    @Test fun `an answer is recorded and the checklist moves to the next screen`() = runBlocking {
        val before = System.currentTimeMillis()
        answer("RN1", cursor = 4, questionId = "exterior-03", status = AnswerStatus.VERIFIED, comment = "  rayure  ", photoPath = "/p/a.jpg", replacedPhoto = null)
        val (saved, next) = repository.answered.single()
        assertEquals(5, next)
        assertEquals("exterior-03", saved.questionId)
        assertEquals(AnswerStatus.VERIFIED, saved.status)
        assertEquals("rayure", saved.comment)
        assertEquals("/p/a.jpg", saved.photoPath)
        assertTrue(saved.answeredAtEpochMs >= before)
        assertTrue(repository.deletedPhotos.isEmpty())
    }

    @Test fun `a blank comment is stored as none`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.SKIPPED, "   ", null, null)
        assertNull(repository.answered.single().first.comment)
    }

    @Test fun `the replaced photo is deleted once the answer points to another one`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.VERIFIED, "", "/p/new.jpg", replacedPhoto = "/p/old.jpg")
        assertEquals(listOf("/p/old.jpg"), repository.deletedPhotos)
    }

    @Test fun `the replaced photo is deleted when the photo is removed`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.VERIFIED, "", null, replacedPhoto = "/p/old.jpg")
        assertEquals(listOf("/p/old.jpg"), repository.deletedPhotos)
    }

    @Test fun `a photo kept by the new answer is not deleted`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.VERIFIED, "", "/p/same.jpg", replacedPhoto = "/p/same.jpg")
        assertTrue(repository.deletedPhotos.isEmpty())
    }

    @Test fun `updating a finished checklist replaces the answer and drops the photo it no longer uses`() = runBlocking {
        update("RN1", "interior-02", AnswerStatus.SKIPPED, " plus tard ", null, replacedPhoto = "/p/old.jpg")
        val saved = repository.updated.single()
        assertEquals("interior-02", saved.questionId)
        assertEquals(AnswerStatus.SKIPPED, saved.status)
        assertEquals("plus tard", saved.comment)
        assertTrue(repository.answered.isEmpty())
        assertEquals(listOf("/p/old.jpg"), repository.deletedPhotos)
    }
}
