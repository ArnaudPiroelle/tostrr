package app.tostrr.ui.update

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.testing.TestGraph
import app.tostrr.ui.theme.TostrrTheme
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class UpdateScreenTest {
    @get:Rule val compose = createComposeRule()

    private val graph = TestGraph()
    private val notes = """
        # Tostrr 0.2.0

        > [!NOTE]
        > À lire d'abord.

        - Une **nouveauté**
    """.trimIndent()

    private var closed = false

    private fun show() {
        graph.updates.update.value = AppUpdate("0.2.0", notes, "https://example.com/a.apk", 3_500_000)
        compose.setContent { TostrrTheme { UpdateScreen(onClose = { closed = true }, viewModel = graph.updateViewModel()) } }
    }

    @Test fun `shows the version and the notes without their Markdown`() {
        show()

        compose.onNodeWithText("Nouvelle version disponible").assertIsDisplayed()
        compose.onNodeWithText("Version 0.2.0", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tostrr 0.2.0").assertIsDisplayed()
        compose.onNodeWithText("À lire d'abord.").assertIsDisplayed()
        compose.onNodeWithText("Une nouveauté").assertIsDisplayed()
        compose.onNodeWithText("[!NOTE]", substring = true).assertDoesNotExist()
    }

    @Test fun `updating opens the system installer on the downloaded APK`() {
        val apk = File(RuntimeEnvironment.getApplication().cacheDir, "updates/tostrr-0.2.0.apk")
        graph.updates.downloading = flow { emit(UpdateDownload.Ready(apk.absolutePath)) }
        show()

        compose.onNodeWithText("Mettre à jour").performClick()
        compose.waitForIdle()

        val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertEquals("content", intent.data?.scheme)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        compose.onNodeWithText("Installer").assertIsDisplayed()
    }

    @Test fun `the progress replaces the button while downloading`() {
        graph.updates.downloading = flow {
            emit(UpdateDownload.Downloading(0.42f))
            awaitCancellation()
        }
        show()

        compose.onNodeWithText("Mettre à jour").performClick()

        compose.onNodeWithText("Téléchargement… 42 %").assertIsDisplayed()
        compose.onNodeWithText("Mettre à jour").assertDoesNotExist()
    }

    @Test fun `a failure says so and offers to try again`() {
        graph.updates.downloading = flow { throw IOException("offline") }
        show()

        compose.onNodeWithText("Mettre à jour").performClick()

        compose.onNodeWithText("Le téléchargement a échoué", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Réessayer").assertIsDisplayed()
    }

    @Test fun `later skips the version and closes`() {
        show()

        compose.onNodeWithText("Plus tard").performClick()
        compose.waitForIdle()

        assertEquals(listOf("0.2.0"), graph.updates.skipped)
        assertTrue(closed)
    }
}
