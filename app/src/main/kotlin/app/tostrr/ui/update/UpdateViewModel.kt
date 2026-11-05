package app.tostrr.ui.update

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.domain.update.usecase.DownloadUpdateUseCase
import app.tostrr.domain.update.usecase.ObserveAvailableUpdateUseCase
import app.tostrr.domain.update.usecase.SkipUpdateUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseSideEffect
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.update.UpdateViewModel.Action
import app.tostrr.ui.update.UpdateViewModel.SideEffect
import app.tostrr.ui.update.UpdateViewModel.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

/** Offers the newer release: downloads its APK, then has the screen open the system installer. */
@KoinViewModel
class UpdateViewModel(
    observeAvailableUpdateUseCase: ObserveAvailableUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val skipUpdateUseCase: SkipUpdateUseCase,
) : BaseViewModel<Action, State, SideEffect>() {

    private val download = MutableStateFlow<Download>(Download.NotStarted)

    override val state: StateFlow<State> = combine(observeAvailableUpdateUseCase(), download, ::State)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            State(observeAvailableUpdateUseCase().value, download.value),
        )

    override suspend fun onHandle(action: Action) {
        when (action) {
            Action.OnUpdate -> update()
            Action.OnLater -> {
                state.value.update?.let { skipUpdateUseCase(it.version) }
                dispatchSideEffect(SideEffect.Close)
            }
        }
    }

    private suspend fun update() {
        val update = state.value.update ?: return
        if (download.value is Download.Running) return
        download.value = Download.Running(0f)
        try {
            downloadUpdateUseCase(update).collect { step ->
                when (step) {
                    is UpdateDownload.Downloading -> download.value = Download.Running(step.progress)
                    is UpdateDownload.Ready -> {
                        download.value = Download.Done
                        dispatchSideEffect(SideEffect.OpenInstaller(step.path))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            download.value = Download.Failed
        }
    }

    sealed interface Download {
        data object NotStarted : Download
        data class Running(val progress: Float) : Download

        /** The APK is there; the user may have left the installer without installing, and can open it again. */
        data object Done : Download
        data object Failed : Download
    }

    data class State(
        /** Null once there is nothing to install, and the screen closes. */
        val update: AppUpdate? = null,
        val download: Download = Download.NotStarted,
    ) : BaseState

    sealed class Action : BaseAction {
        data object OnUpdate : Action()
        data object OnLater : Action()
    }

    sealed class SideEffect : BaseSideEffect {
        data class OpenInstaller(val path: String) : SideEffect()
        data object Close : SideEffect()
    }
}
