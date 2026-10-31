package app.tostrr.ui.login

import app.tostrr.domain.auth.usecase.CompleteLoginUseCase
import app.tostrr.domain.auth.usecase.ObserveLoggedInUseCase
import app.tostrr.domain.auth.usecase.StartLoginUseCase
import app.tostrr.domain.order.usecase.RefreshOrdersUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseSideEffect
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel2
import app.tostrr.ui.common.UiError
import app.tostrr.ui.common.toUiError
import app.tostrr.ui.login.LoginViewModel.Action
import app.tostrr.ui.login.LoginViewModel.SideEffect
import app.tostrr.ui.login.LoginViewModel.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** Logs in to Tesla: opens its login page, then exchanges the redirect it sends back. */
@KoinViewModel
class LoginViewModel(
    private val observeLoggedInUseCase: ObserveLoggedInUseCase,
    private val startLoginUseCase: StartLoginUseCase,
    private val completeLoginUseCase: CompleteLoginUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase,
    /** The first sync runs here: this screen leaves as soon as the session opens, cancelling its own scope. */
    private val appScope: CoroutineScope,
) : BaseViewModel2<Action, State, SideEffect>(State()) {

    override suspend fun onHandle(action: Action) {
        when (action) {
            Action.OnLogin -> {
                updateState { it.copy(error = null) }
                dispatchSideEffect(SideEffect.OpenLoginPage(startLoginUseCase()))
            }
            is Action.OnRedirect -> completeLogin(action.redirect)
        }
    }

    private suspend fun completeLogin(redirect: String) {
        // A redirect delivered twice (deep link, then a paste) would fail: its code was already exchanged.
        if (observeLoggedInUseCase().value || state.value.busy) return
        updateState { it.copy(busy = true, error = null) }
        try {
            completeLoginUseCase(redirect)
            appScope.launch { runCatching { refreshOrdersUseCase() } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateState { it.copy(error = e.toUiError()) }
        } finally {
            updateState { it.copy(busy = false) }
        }
    }

    data class State(
        val busy: Boolean = false,
        val error: UiError? = null,
    ) : BaseState

    sealed class SideEffect : BaseSideEffect {
        /** Tesla's login page, to open in a browser tab. */
        data class OpenLoginPage(val url: String) : SideEffect()
    }

    sealed class Action : BaseAction {
        data object OnLogin : Action()

        /** The `tesla://auth/callback?...` URL, from the deep link or pasted. */
        data class OnRedirect(val redirect: String) : Action()
    }
}
