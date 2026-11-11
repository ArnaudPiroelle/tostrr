package app.tostrr.domain.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChecklistTest {
    @Test
    fun `catalogue holds the 81 questions of the five sections`() {
        assertEquals(listOf(26, 14, 14, 18, 9), DeliveryChecklist.sections.map { it.questions.size })
        assertEquals(81, DeliveryChecklist.questionCount)
    }

    @Test
    fun `question ids are unique`() {
        val ids = DeliveryChecklist.sections.flatMap { s -> s.questions.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `each section opens with an intro followed by its numbered questions`() {
        val steps = DeliveryChecklist.steps
        assertEquals(81 + 5, steps.size)
        assertTrue(steps[0] is ChecklistStep.SectionIntro)
        val last = steps.last() as ChecklistStep.Question
        assertEquals(9, last.number)
        assertEquals("accessories", last.section.id)
        val secondIntro = steps[27] as ChecklistStep.SectionIntro
        assertEquals("interior", secondIntro.section.id)
    }

    @Test
    fun `a question id leads to the screen asking it`() {
        val index = DeliveryChecklist.stepIndexOf("interior-03")
        assertEquals(30, index)
        assertEquals("interior-03", (DeliveryChecklist.steps[index] as ChecklistStep.Question).question.id)
        assertEquals(-1, DeliveryChecklist.stepIndexOf("unknown"))
    }

    @Test
    fun `score counts only verified answers out of every question`() {
        val answers = DeliveryChecklist.sections.flatMap { it.questions }.take(30).mapIndexed { i, q ->
            q.id to ChecklistAnswer(q.id, if (i < 22) AnswerStatus.VERIFIED else AnswerStatus.SKIPPED, answeredAtEpochMs = 0)
        }.toMap()
        val checklist = Checklist("RN1", 0, 1, DeliveryChecklist.steps.size, answers)
        assertEquals(22, checklist.verifiedCount)
        assertEquals(27, checklist.verifiedPercent)
    }

    @Test
    fun `problems are counted apart and against the score`() {
        val answers = DeliveryChecklist.sections.flatMap { it.questions }.take(10).mapIndexed { i, q ->
            q.id to ChecklistAnswer(q.id, if (i < 3) AnswerStatus.PROBLEM else AnswerStatus.VERIFIED, answeredAtEpochMs = 0)
        }.toMap()
        val checklist = Checklist("RN1", 0, 1, DeliveryChecklist.steps.size, answers)
        assertEquals(3, checklist.problemCount)
        assertEquals(7, checklist.verifiedCount)
        assertEquals(8, checklist.verifiedPercent)
    }
}
