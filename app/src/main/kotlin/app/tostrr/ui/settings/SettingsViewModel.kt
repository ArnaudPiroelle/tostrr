package app.tostrr.ui.settings

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.appearance.usecase.ObserveThemeModeUseCase
import app.tostrr.domain.appearance.usecase.SetThemeModeUseCase
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.auth.usecase.LoadTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.auth.usecase.ObserveTeslaAccountUseCase
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.notification.usecase.ObserveNotificationSettingsUseCase
import app.tostrr.domain.notification.usecase.SetNotificationEnabledUseCase
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.usecase.ObserveSyncSettingsUseCase
import app.tostrr.domain.sync.usecase.SetAutoSyncUseCase
import app.tostrr.domain.sync.usecase.SetSyncIntervalUseCase
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateCheck
import app.tostrr.domain.update.usecase.CheckForUpdateNowUseCase
import app.tostrr.domain.update.usecase.ObserveAvailableUpdateUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseSideEffect
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.settings.SettingsViewModel.Action
import app.tostrr.ui.settings.SettingsViewModel.SideEffect
import app.tostrr.ui.settings.SettingsViewModel.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** The app settings, one group per section of the screen. */
@KoinViewModel
class SettingsViewModel(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeSyncSettingsUseCase: ObserveSyncSettingsUseCase,
    observeNotificationSettingsUseCase: ObserveNotificationSettingsUseCase,
    observeTeslaAccountUseCase: ObserveTeslaAccountUseCase,
    observeAvailableUpdateUseCase: ObserveAvailableUpdateUseCase,
    loadTeslaAccountUseCase: LoadTeslaAccountUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setAutoSyncUseCase: SetAutoSyncUseCase,
    private val setSyncIntervalUseCase: SetSyncIntervalUseCase,
    private val setNotificationEnabledUseCase: SetNotificationEnabledUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val checkForUpdateNowUseCase: CheckForUpdateNowUseCase,
) : BaseViewModel<Action, State, SideEffect>() {

    /** The check asked for from this screen: whether one is running, and what the last one found. */
    private val manualCheck = MutableStateFlow(ManualCheck())

    override val state: StateFlow<State> = combine(
        combine(
            observeThemeModeUseCase(),
            observeSyncSettingsUseCase(),
            observeNotificationSettingsUseCase(),
            observeTeslaAccountUseCase(),
            observeAvailableUpdateUseCase(),
            ::State,
        ),
        manualCheck,
    ) { state, check -> state.copy(checkingUpdate = check.running, updateCheck = check.result) }
        // The profile is fetched once the screen watches it; it lands through the account flow.
        .onStart { viewModelScope.launch { loadTeslaAccountUseCase() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            State(
                observeThemeModeUseCase().value,
                observeSyncSettingsUseCase().value,
                observeNotificationSettingsUseCase().value,
                observeTeslaAccountUseCase().value,
                observeAvailableUpdateUseCase().value,
            ),
        )

    override suspend fun onHandle(action: Action) {
        when (action) {
            is Action.OnThemeSelected -> setThemeModeUseCase(action.mode)
            is Action.OnAutoSyncChanged -> setAutoSyncUseCase(action.enabled)
            is Action.OnIntervalSelected -> setSyncIntervalUseCase(action.interval)
            is Action.OnNotificationChanged -> setNotificationEnabledUseCase(action.kind, action.enabled)
            Action.OnLogout -> logoutUseCase()
            Action.OnCheckForUpdate -> checkForUpdate()
        }
    }

    private suspend fun checkForUpdate() {
        if (manualCheck.value.running) return
        manualCheck.value = ManualCheck(running = true)
        val result = checkForUpdateNowUseCase()
        manualCheck.value = ManualCheck(result = result)
        if (result == UpdateCheck.AVAILABLE) dispatchSideEffect(SideEffect.OpenUpdate)
    }

    private data class ManualCheck(val running: Boolean = false, val result: UpdateCheck? = null)

    data class State(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val sync: SyncSettings = SyncSettings(),
        val notifications: NotificationSettings = NotificationSettings(),
        /** Null until the profile is loaded, or if Tesla didn't give it. */
        val account: TeslaAccount? = null,
        /** The newer release, even one left for later. */
        val update: AppUpdate? = null,
        /** A check asked for from this screen is running. */
        val checkingUpdate: Boolean = false,
        /** What the last check asked for from this screen found; null until there was one. */
        val updateCheck: UpdateCheck? = null,
    ) : BaseState

    sealed class Action : BaseAction {
        data class OnThemeSelected(val mode: ThemeMode) : Action()
        data class OnAutoSyncChanged(val enabled: Boolean) : Action()
        data class OnIntervalSelected(val interval: SyncInterval) : Action()
        data class OnNotificationChanged(val kind: NotificationKind, val enabled: Boolean) : Action()
        data object OnLogout : Action()
        data object OnCheckForUpdate : Action()
    }

    sealed class SideEffect : BaseSideEffect {
        /** A check found a newer release. */
        data object OpenUpdate : SideEffect()
    }
}
