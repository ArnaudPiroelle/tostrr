package app.tostrr.ui.orders

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.error.ApiException
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.error.SessionExpiredException
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.domain.order.usecase.RefreshOrdersUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoSideEffect
import app.tostrr.ui.orders.OrdersViewModel.Action
import app.tostrr.ui.orders.OrdersViewModel.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

/** Minimum delay between two refreshes, to avoid hammering Tesla's private API. */
const val REFRESH_COOLDOWN_MS = 30_000L

/** The orders list, refreshed on demand. */
@KoinViewModel
class OrdersViewModel(
    observeOrdersUseCase: ObserveOrdersUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase,
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<Action, State, NoSideEffect>() {

    private val sync = MutableStateFlow(Sync())

    override val state: StateFlow<State> = combine(observeOrdersUseCase(), sync, ::State)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State(observeOrdersUseCase().value, sync.value))

    override suspend fun onHandle(action: Action) {
        when (action) {
            Action.OnRefresh -> refresh()
        }
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        if (sync.value.refreshing || now - sync.value.lastRefreshAtMs < REFRESH_COOLDOWN_MS) return
        sync.update { it.copy(refreshing = true, lastRefreshAtMs = now, error = null) }
        try {
            refreshOrdersUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionExpiredException) {
            logoutUseCase()
            sync.update { it.copy(error = e.message) }
        } catch (e: Exception) {
            // Auth and API errors carry a message meant for the user; anything else gets context.
            val message = if (e is AuthException || e is ApiException) e.message else "Synchronisation impossible : ${e.message}"
            sync.update { it.copy(error = message) }
        } finally {
            sync.update { it.copy(refreshing = false) }
        }
    }

    /** The manual refresh, the only state this screen keeps itself. */
    data class Sync(
        val refreshing: Boolean = false,
        /** Epoch ms of the last refresh attempt; 0 when none yet. Drives the refresh cooldown. */
        val lastRefreshAtMs: Long = 0L,
        val error: String? = null,
    )

    data class State(
        val orders: List<Order> = emptyList(),
        val sync: Sync = Sync(),
    ) : BaseState

    sealed class Action : BaseAction {
        data object OnRefresh : Action()
    }
}
