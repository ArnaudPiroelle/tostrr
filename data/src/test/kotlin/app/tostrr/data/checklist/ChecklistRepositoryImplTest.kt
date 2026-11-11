package app.tostrr.data.checklist

import androidx.room.Room
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.ChecklistAnswer
import app.tostrr.domain.checklist.DeliveryChecklist
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ChecklistRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val database = Room.inMemoryDatabaseBuilder(context, TostrrDatabase::class.java).allowMainThreadQueries().build()
    private val repository = ChecklistRepositoryImpl(database.checklistDao(), context)
    private val photoRoot = File(context.filesDir, "checklist_photos")
    private val lastStep = DeliveryChecklist.steps.size

    @After fun tearDown() = database.close()

    private suspend fun checklist(ref: String = REF) = repository.observe(ref).first()

    private fun answer(id: String, status: AnswerStatus = AnswerStatus.VERIFIED, at: Long = 10L, photos: List<String> = emptyList()) =
        ChecklistAnswer(id, status, comment = "ok", photoPaths = photos, answeredAtEpochMs = at)

    @Test fun `there is no checklist until it is started`() = runTest {
        assertNull(checklist())
        repository.start(REF, now = 5L)
        val started = checklist()!!
        assertEquals(0, started.cursor)
        assertEquals(5L, started.startedAtEpochMs)
        assertFalse(started.completed)
    }

    @Test fun `starting again leaves the checklist as it is`() = runTest {
        repository.start(REF, now = 5L)
        repository.moveTo(REF, 3, now = 6L)
        repository.start(REF, now = 7L)
        assertEquals(3, checklist()!!.cursor)
        assertEquals(5L, checklist()!!.startedAtEpochMs)
    }

    @Test fun `an answer is recorded and moves the checklist on`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("exterior-01"), nextCursor = 2)
        val after = checklist()!!
        assertEquals(2, after.cursor)
        assertEquals(answer("exterior-01"), after.answers["exterior-01"])
    }

    @Test fun `answering again replaces the answer`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("exterior-01"), nextCursor = 2)
        repository.answer(REF, answer("exterior-01", AnswerStatus.SKIPPED), nextCursor = 2)
        assertEquals(AnswerStatus.SKIPPED, checklist()!!.answers.getValue("exterior-01").status)
        assertEquals(1, checklist()!!.answers.size)
    }

    @Test fun `moving past the last step completes the checklist, which then stays put`() = runTest {
        repository.start(REF)
        repository.moveTo(REF, lastStep + 10, now = 99L)
        val done = checklist()!!
        assertEquals(lastStep, done.cursor)
        assertEquals(99L, done.completedAtEpochMs)
        repository.moveTo(REF, 0)
        repository.answer(REF, answer("exterior-01"), nextCursor = 0)
        assertEquals(lastStep, checklist()!!.cursor)
        assertTrue(checklist()!!.answers.isEmpty())
    }

    @Test fun `the last answer completes the checklist at its time`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("accessories-09", at = 42L), nextCursor = lastStep)
        assertEquals(42L, checklist()!!.completedAtEpochMs)
    }

    @Test fun `moving or answering a checklist never started does nothing`() = runTest {
        repository.moveTo(REF, 3)
        repository.answer(REF, answer("exterior-01"), nextCursor = 2)
        repository.reopen(REF, 1)
        assertNull(checklist())
    }

    @Test fun `reopening keeps the answers and clamps the cursor`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("exterior-01"), nextCursor = lastStep)
        repository.reopen(REF, lastStep + 5)
        val reopened = checklist()!!
        assertFalse(reopened.completed)
        assertEquals(DeliveryChecklist.steps.lastIndex, reopened.cursor)
        assertEquals(setOf("exterior-01"), reopened.answers.keys)
    }

    @Test fun `an answer can only be updated once the checklist is finished, which it stays`() = runTest {
        repository.start(REF)
        repository.updateAnswer(REF, answer("exterior-01"))
        assertTrue(checklist()!!.answers.isEmpty())

        repository.moveTo(REF, lastStep, now = 50L)
        repository.updateAnswer(REF, answer("exterior-01", AnswerStatus.SKIPPED))
        val updated = checklist()!!
        assertEquals(AnswerStatus.SKIPPED, updated.answers.getValue("exterior-01").status)
        assertEquals(50L, updated.completedAtEpochMs)
    }

    @Test fun `an answer keeps its photos in their order`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("exterior-01", photos = listOf("/p/b.jpg", "/p/a.jpg", "/p/c.jpg")), nextCursor = 2)
        assertEquals(listOf("/p/b.jpg", "/p/a.jpg", "/p/c.jpg"), checklist()!!.answers.getValue("exterior-01").photoPaths)
    }

    @Test fun `only a completed checklist can be locked, once`() = runTest {
        repository.start(REF)
        repository.lock(REF, now = 20L)
        assertFalse(checklist()!!.locked)
        repository.moveTo(REF, lastStep, now = 30L)
        repository.lock(REF, now = 40L)
        repository.lock(REF, now = 50L)
        assertEquals(40L, checklist()!!.lockedAtEpochMs)
    }

    @Test fun `a locked checklist can't be reopened nor its answers changed, only deleted`() = runTest {
        repository.start(REF)
        repository.answer(REF, answer("exterior-01"), nextCursor = 2)
        repository.moveTo(REF, lastStep, now = 30L)
        repository.lock(REF, now = 40L)

        assertFalse(repository.updateAnswer(REF, answer("exterior-01", AnswerStatus.SKIPPED)))
        assertFalse(repository.answer(REF, answer("exterior-02"), nextCursor = 3))
        repository.reopen(REF, 0)
        val locked = checklist()!!
        assertTrue(locked.completed)
        assertEquals(AnswerStatus.VERIFIED, locked.answers.getValue("exterior-01").status)
        assertFalse("exterior-02" in locked.answers)

        repository.delete(REF)
        assertNull(checklist())
    }

    @Test fun `checklists of different orders are kept apart`() = runTest {
        repository.start(REF)
        repository.start("RN2")
        repository.answer("RN2", answer("exterior-01"), nextCursor = 2)
        assertTrue(checklist()!!.answers.isEmpty())
        assertEquals(1, checklist("RN2")!!.answers.size)
    }

    @Test fun `deleting removes the checklist, its answers and its photos`() = runTest {
        repository.start(REF)
        val photo = repository.importPhoto(REF, "exterior-01", "jpeg".byteInputStream())
        repository.answer(REF, answer("exterior-01", photos = listOf(photo)), nextCursor = 2)
        repository.delete(REF)
        assertNull(checklist())
        assertFalse(File(photo).exists())
        assertFalse(File(photoRoot, REF).exists())
    }

    @Test fun `an imported photo is copied into the order's photo folder`() = runTest {
        val path = repository.importPhoto(REF, "exterior-01", "jpeg bytes".byteInputStream())
        val file = File(path)
        assertEquals(File(photoRoot, REF).canonicalPath, file.parentFile!!.canonicalPath)
        assertTrue(file.name.startsWith("exterior-01-"))
        assertEquals("jpeg bytes", file.readText())
    }

    @Test fun `photos are deleted only inside the photo folder`() = runTest {
        val kept = File(context.filesDir, "orders.json").apply { writeText("[]") }
        repository.deletePhoto(kept.path)
        repository.deletePhoto(File(photoRoot, "../orders.json").path)
        assertTrue(kept.exists())

        val photo = repository.importPhoto(REF, "exterior-01", "x".byteInputStream())
        repository.deletePhoto(photo)
        assertFalse(File(photo).exists())
    }

    private companion object {
        const val REF = "RN1"
    }
}
