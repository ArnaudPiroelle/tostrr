package app.tostrr.ui

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.auth.usecase.ObserveLoggedInUseCase
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.ui.MainViewModel.State
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoAction
import app.tostrr.ui.architecture.NoSideEffect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

/** What the activity needs to pick the screens: the session, and which orders a deep link can open. */
@KoinViewModel
class MainViewModel(
    observeLoggedInUseCase: ObserveLoggedInUseCase,
    observeOrdersUseCase: ObserveOrdersUseCase,
) : BaseViewModel<NoAction, State, NoSideEffect>() {

    // Starts from the current values: the first screen of the back stack depends on the session.
    override val state: StateFlow<State> = combine(observeLoggedInUseCase(), observeOrdersUseCase(), ::toState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toState(observeLoggedInUseCase().value, observeOrdersUseCase().value),
        )

    override suspend fun onHandle(action: NoAction) = Unit

    private fun toState(loggedIn: Boolean, orders: List<Order>) = State(loggedIn, orders.map { it.reference }.toSet())

    data class State(
        /** Logging in or out flips it; the activity then swaps the whole back stack. */
        val loggedIn: Boolean = false,
        val orderReferences: Set<String> = emptySet(),
    ) : BaseState
}
