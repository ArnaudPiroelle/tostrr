package app.tostrr.domain.checklist

import app.tostrr.domain.checklist.usecase.AnswerChecklistQuestionUseCase
import app.tostrr.domain.checklist.usecase.CreateChecklistReportUseCase
import app.tostrr.domain.checklist.usecase.UpdateChecklistAnswerUseCase
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream

class ChecklistUseCasesTest {
    /** Keeps what was answered and which photos were deleted; [accepts] false plays a locked checklist. */
    private class FakeChecklists : ChecklistRepository {
        val answered = mutableListOf<Pair<ChecklistAnswer, Int>>()
        val updated = mutableListOf<ChecklistAnswer>()
        val deletedPhotos = mutableListOf<String>()
        var accepts = true
        var stored: Checklist? = null

        override fun observe(ref: String): Flow<Checklist?> = flowOf(stored)
        override suspend fun start(ref: String, now: Long) = Unit
        override suspend fun moveTo(ref: String, cursor: Int, now: Long) = Unit
        override suspend fun lock(ref: String, now: Long) = Unit
        override suspend fun reopen(ref: String, cursor: Int) = Unit
        override suspend fun updateAnswer(ref: String, answer: ChecklistAnswer): Boolean {
            if (accepts) updated += answer
            return accepts
        }
        override suspend fun answer(ref: String, answer: ChecklistAnswer, nextCursor: Int): Boolean {
            if (accepts) answered += answer to nextCursor
            return accepts
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

    @Test fun `an answer is recorded with its photos and the checklist moves to the next screen`() = runBlocking {
        val before = System.currentTimeMillis()
        answer("RN1", cursor = 4, questionId = "exterior-03", status = AnswerStatus.VERIFIED, comment = "  rayure  ",
            photoPaths = listOf("/p/a.jpg", "/p/b.jpg", "/p/a.jpg"), previousPhotos = emptyList())
        val (saved, next) = repository.answered.single()
        assertEquals(5, next)
        assertEquals("exterior-03", saved.questionId)
        assertEquals(AnswerStatus.VERIFIED, saved.status)
        assertEquals("rayure", saved.comment)
        assertEquals(listOf("/p/a.jpg", "/p/b.jpg"), saved.photoPaths)
        assertTrue(saved.answeredAtEpochMs >= before)
        assertTrue(repository.deletedPhotos.isEmpty())
    }

    @Test fun `a blank comment is stored as none`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.SKIPPED, "   ", emptyList(), emptyList())
        assertNull(repository.answered.single().first.comment)
    }

    @Test fun `only the photos the new answer dropped are deleted`() = runBlocking {
        answer("RN1", 0, "exterior-01", AnswerStatus.VERIFIED, "", listOf("/p/kept.jpg", "/p/new.jpg"), previousPhotos = listOf("/p/old.jpg", "/p/kept.jpg"))
        assertEquals(listOf("/p/old.jpg"), repository.deletedPhotos)
    }

    @Test fun `an answer the checklist refused deletes nothing`() = runBlocking {
        repository.accepts = false
        answer("RN1", 0, "exterior-01", AnswerStatus.VERIFIED, "", emptyList(), previousPhotos = listOf("/p/old.jpg"))
        update("RN1", "exterior-01", AnswerStatus.VERIFIED, "", emptyList(), previousPhotos = listOf("/p/old.jpg"))
        assertTrue(repository.deletedPhotos.isEmpty())
    }

    @Test fun `updating a finished checklist replaces the answer and drops the photos it no longer uses`() = runBlocking {
        update("RN1", "interior-02", AnswerStatus.SKIPPED, " plus tard ", emptyList(), previousPhotos = listOf("/p/old.jpg"))
        val saved = repository.updated.single()
        assertEquals("interior-02", saved.questionId)
        assertEquals(AnswerStatus.SKIPPED, saved.status)
        assertEquals("plus tard", saved.comment)
        assertTrue(repository.answered.isEmpty())
        assertEquals(listOf("/p/old.jpg"), repository.deletedPhotos)
    }

    @Test fun `the report is only written for a locked checklist`() = runBlocking {
        val written = mutableListOf<Checklist>()
        val writer = object : ChecklistReportWriter {
            override suspend fun write(checklist: Checklist, order: Order?): File = File("report.pdf").also { written += checklist }
        }
        val orders = object : OrderRepository {
            override val orders = MutableStateFlow(emptyList<Order>())
            override val history = MutableStateFlow(emptyMap<String, List<Change>>())
            override val syncing = MutableStateFlow(false)
            override fun lastSyncAgeMs(now: Long): Long? = null
            override suspend fun refresh() = Unit
            override fun clear() = Unit
        }
        val report = CreateChecklistReportUseCase(repository, orders, writer)
        repository.stored = Checklist("RN1", 1L, completedAtEpochMs = 2L, cursor = 0, answers = emptyMap())
        assertNull(report("RN1"))
        repository.stored = repository.stored!!.copy(lockedAtEpochMs = 3L)
        assertEquals(File("report.pdf"), report("RN1"))
        assertEquals(listOf(repository.stored), written)
    }
}
