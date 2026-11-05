package app.tostrr.ui

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.appearance.usecase.ObserveThemeModeUseCase
import app.tostrr.domain.auth.usecase.ObserveLoggedInUseCase
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.usecase.CheckForUpdateUseCase
import app.tostrr.domain.update.usecase.ObserveUpdateToOfferUseCase
import app.tostrr.ui.MainViewModel.State
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoAction
import app.tostrr.ui.architecture.NoSideEffect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** What the activity needs to pick the screens: the session, which orders a deep link can open, the theme and any update. */
@KoinViewModel
class MainViewModel(
    observeLoggedInUseCase: ObserveLoggedInUseCase,
    observeOrdersUseCase: ObserveOrdersUseCase,
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeUpdateToOfferUseCase: ObserveUpdateToOfferUseCase,
    checkForUpdateUseCase: CheckForUpdateUseCase,
) : BaseViewModel<NoAction, State, NoSideEffect>() {

    // Starts from the current values: the first screen of the back stack depends on the session.
    override val state: StateFlow<State> = combine(
        observeLoggedInUseCase(),
        observeOrdersUseCase(),
        observeThemeModeUseCase(),
        observeUpdateToOfferUseCase(),
        ::toState,
    )
        // A new release is looked for whenever the app comes to the foreground; it lands through the update flow.
        .onStart { viewModelScope.launch { checkForUpdateUseCase() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toState(
                observeLoggedInUseCase().value,
                observeOrdersUseCase().value,
                observeThemeModeUseCase().value,
                observeUpdateToOfferUseCase().value,
            ),
        )

    override suspend fun onHandle(action: NoAction) = Unit

    private fun toState(loggedIn: Boolean, orders: List<Order>, themeMode: ThemeMode, update: AppUpdate?) =
        State(loggedIn, orders.map { it.reference }.toSet(), themeMode, update?.version)

    data class State(
        /** Logging in or out flips it; the activity then swaps the whole back stack. */
        val loggedIn: Boolean = false,
        val orderReferences: Set<String> = emptySet(),
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        /** The version to bring up on its own, if a newer one is published and wasn't skipped. */
        val availableUpdate: String? = null,
    ) : BaseState
}
