package app.tostrr.ui.checklist

import java.io.File
import android.os.ParcelFileDescriptor
import android.net.Uri
import android.database.Cursor
import android.content.ContentValues
import android.content.ContentProvider
import app.tostrr.domain.checklist.Checklist
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.order
import app.tostrr.testing.sideEffects
import app.tostrr.testing.watch
import app.tostrr.ui.checklist.ChecklistViewModel.Action
import app.tostrr.ui.checklist.ChecklistViewModel.SideEffect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ChecklistViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()
    private fun viewModel() = graph.checklistViewModel("RN1", RuntimeEnvironment.getApplication())

    @Test fun `no report until the checklist is locked`() = runTest {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnShareReport)

        assertTrue(effects.isEmpty())
        assertTrue(graph.reports.written.isEmpty())
    }

    @Test fun `a locked checklist's report is written for its order, to open or to share`() = runTest {
        graph.orders.orders.value = listOf(order("RN1"))
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap()))
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnLock)
        viewModel.handle(Action.OnOpenReport)
        viewModel.handle(Action.OnShareReport)

        val (checklist, order) = graph.reports.written.first()
        assertTrue(checklist.locked)
        assertEquals("RN1", order?.reference)
        assertTrue(effects[0] is SideEffect.OpenReport)
        assertTrue(effects[1] is SideEffect.ShareReport)
    }

    @Test fun `a locked checklist has no question being changed`() = runTest {
        graph.checklists.checklists.value = mapOf("RN1" to Checklist("RN1", 0L, 1L, 0, emptyMap(), lockedAtEpochMs = 2L))
        val viewModel = viewModel()
        watch(viewModel)

        viewModel.handle(Action.OnEditQuestion("exterior-01"))

        assertNull(viewModel.state.value.editingQuestionId)
    }

    @Test fun `a picked image another app serves is imported`() = runTest {
        val picked = File(context.cacheDir, "picked.jpg").apply { writeText("jpeg bytes") }
        Robolectric.setupContentProvider(PickerProvider::class.java, "media.example")
        PickerProvider.file = picked
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnImportPhoto("exterior-01", Uri.parse("content://media.example/picked.jpg")))

        val imported = effects.single() as SideEffect.PhotoImported
        assertEquals("jpeg bytes", File(imported.path).readText())
    }

    @Test fun `a picked URI that would read the app's own files is ignored`() = runTest {
        val private = File(context.filesDir, "orders.json").apply { writeText("[]") }
        val viewModel = viewModel()
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnImportPhoto("exterior-01", Uri.fromFile(private)))
        viewModel.handle(Action.OnImportPhoto("exterior-01", Uri.parse("content://${context.packageName}.fileprovider/photos/RN1/x.jpg")))

        assertTrue(effects.isEmpty())
    }

    /** Stands for the photo picker's provider: serves one image. */
    class PickerProvider : ContentProvider() {
        override fun onCreate() = true
        override fun getType(uri: Uri) = "image/jpeg"
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

        companion object {
            lateinit var file: File
        }
    }

    private val context get() = RuntimeEnvironment.getApplication()
}
