package app.tostrr.ui.update

import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.testing.FakeUpdateRepository
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.testing.sideEffects
import app.tostrr.testing.watch
import app.tostrr.ui.update.UpdateViewModel.Action
import app.tostrr.ui.update.UpdateViewModel.Download
import app.tostrr.ui.update.UpdateViewModel.SideEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class UpdateViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()
    private val update = AppUpdate("0.2.0", "Notes", "https://example.com/a.apk", 100)

    @Test fun `the offered update shows at once`() {
        graph.updates.update.value = update
        assertEquals(update, graph.updateViewModel().state.value.update)
    }

    @Test fun `updating shows the progress, then opens the installer`() = runTest {
        graph.updates.update.value = update
        val steps = MutableSharedFlow<UpdateDownload?>()
        graph.updates.downloading = flow { steps.takeWhile { it != null }.collect { emit(it!!) } }
        val viewModel = graph.updateViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnUpdate)
        assertEquals(Download.Running(0f), viewModel.state.value.download)

        steps.emit(UpdateDownload.Downloading(0.5f))
        assertEquals(Download.Running(0.5f), viewModel.state.value.download)

        steps.emit(UpdateDownload.Ready(FakeUpdateRepository.APK_PATH))
        assertEquals(Download.Done, viewModel.state.value.download)
        assertEquals(listOf<SideEffect>(SideEffect.OpenInstaller(FakeUpdateRepository.APK_PATH)), effects)
    }

    @Test fun `the installer can be opened again after leaving it`() = runTest {
        graph.updates.update.value = update
        val viewModel = graph.updateViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnUpdate)
        viewModel.handle(Action.OnUpdate)

        assertEquals(2, effects.size)
    }

    @Test fun `a failed download can be tried again`() = runTest {
        graph.updates.update.value = update
        graph.updates.downloading = flow { throw IOException("offline") }
        val viewModel = graph.updateViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnUpdate)
        assertEquals(Download.Failed, viewModel.state.value.download)
        assertEquals(emptyList<SideEffect>(), effects)

        graph.updates.downloading = flow { emit(UpdateDownload.Ready(FakeUpdateRepository.APK_PATH)) }
        viewModel.handle(Action.OnUpdate)
        assertEquals(Download.Done, viewModel.state.value.download)
    }

    @Test fun `later skips this version and closes`() = runTest {
        graph.updates.update.value = update
        val viewModel = graph.updateViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnLater)

        assertEquals(listOf("0.2.0"), graph.updates.skipped)
        assertEquals(listOf<SideEffect>(SideEffect.Close), effects)
    }

    @Test fun `a skipped version can still be installed`() {
        graph.updates.update.value = update
        graph.updates.skip("0.2.0")

        assertEquals(update, graph.updateViewModel().state.value.update)
    }

    @Test fun `nothing to install once up to date`() {
        assertNull(graph.updateViewModel().state.value.update)
    }
}
