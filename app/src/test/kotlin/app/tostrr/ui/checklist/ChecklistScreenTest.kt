package app.tostrr.ui.checklist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.tostrr.domain.checklist.AnswerStatus
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.ChecklistStep
import app.tostrr.domain.checklist.DeliveryChecklist
import app.tostrr.testing.TestGraph
import app.tostrr.ui.theme.TostrrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ChecklistScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph()
    private var closed = false

    private fun show() = compose.setContent {
        TostrrTheme {
            ChecklistScreen(
                orderReference = "RN1",
                onClose = { closed = true },
                viewModel = graph.checklistViewModel("RN1", RuntimeEnvironment.getApplication()),
            )
        }
    }

    private val firstQuestion = DeliveryChecklist.steps.filterIsInstance<ChecklistStep.Question>().first()

    @Test fun `starting goes through the first section to its first question`() {
        show()

        compose.onNodeWithText("Commencer").performScrollTo().performClick()
        assertEquals(0, graph.checklists.checklists.value["RN1"]?.cursor)
        compose.onNodeWithText("Commencer").performScrollTo().performClick()

        // First question of the exterior section, in French (see robolectric.properties).
        compose.onNodeWithText("Le numéro VIN sur le pare-brise correspond-il au certificat d'immatriculation ?").assertIsDisplayed()
    }

    @Test fun `answering records the answer and moves on`() {
        val cursor = DeliveryChecklist.stepIndexOf(firstQuestion.question.id)
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, null, cursor, emptyMap()))
        show()

        compose.onNodeWithText("J'ai vérifié 👍").performScrollTo().performClick()

        val checklist = graph.checklists.checklists.value.getValue("RN1")
        assertEquals(AnswerStatus.VERIFIED, checklist.answers[firstQuestion.question.id]?.status)
        assertEquals(cursor + 1, checklist.cursor)
    }

    @Test fun `locking a finished checklist asks first, then offers the report instead of changes`() {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        show()

        compose.onNodeWithText("Verrouiller et créer le rapport").performScrollTo().performClick()
        compose.onNode(hasText("Verrouiller") and hasAnyAncestor(isDialog())).performClick()

        compose.waitForIdle()
        assertTrue(graph.checklists.checklists.value.getValue("RN1").locked)
        compose.onNodeWithText("Voir le rapport").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Partager le rapport").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Modifier la checklist").assertDoesNotExist()
    }

    @Test fun `a locked checklist's answers can't be opened to change them`() {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap(), lockedAtEpochMs = 2L))
        show()

        compose.onNodeWithText("Le numéro VIN sur le pare-brise correspond-il au certificat d'immatriculation ?").performScrollTo().performClick()

        compose.onNodeWithText("J'ai vérifié 👍").assertDoesNotExist()
    }

    @Test fun `a question keeps every photo of its answer`() {
        val cursor = DeliveryChecklist.stepIndexOf(firstQuestion.question.id)
        val photos = listOf("/p/a.jpg", "/p/b.jpg")
        val answer = app.tostrr.domain.checklist.ChecklistAnswer(firstQuestion.question.id, AnswerStatus.VERIFIED, photoPaths = photos, answeredAtEpochMs = 1L)
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, null, cursor, mapOf(answer.questionId to answer)))
        show()

        compose.onNodeWithText("J'ai vérifié 👍").performScrollTo().performClick()

        assertEquals(photos, graph.checklists.checklists.value.getValue("RN1").answers.getValue(firstQuestion.question.id).photoPaths)
        assertTrue(graph.checklists.deletedPhotos.isEmpty())
    }

    @Test fun `deleting a finished checklist asks first, then closes the screen`() {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        show()

        compose.onNodeWithText("Supprimer la checklist").performScrollTo().performClick()
        compose.onNode(hasText("Supprimer") and hasAnyAncestor(isDialog())).performClick()

        compose.waitForIdle()
        assertNull(graph.checklists.checklists.value["RN1"])
        assertTrue(closed)
    }
}
