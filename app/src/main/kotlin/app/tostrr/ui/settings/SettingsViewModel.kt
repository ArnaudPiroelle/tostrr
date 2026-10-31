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
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoSideEffect
import app.tostrr.ui.settings.SettingsViewModel.Action
import app.tostrr.ui.settings.SettingsViewModel.State
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
    loadTeslaAccountUseCase: LoadTeslaAccountUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setAutoSyncUseCase: SetAutoSyncUseCase,
    private val setSyncIntervalUseCase: SetSyncIntervalUseCase,
    private val setNotificationEnabledUseCase: SetNotificationEnabledUseCase,
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<Action, State, NoSideEffect>() {

    override val state: StateFlow<State> = combine(
        observeThemeModeUseCase(),
        observeSyncSettingsUseCase(),
        observeNotificationSettingsUseCase(),
        observeTeslaAccountUseCase(),
        ::State,
    )
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
            ),
        )

    override suspend fun onHandle(action: Action) {
        when (action) {
            is Action.OnThemeSelected -> setThemeModeUseCase(action.mode)
            is Action.OnAutoSyncChanged -> setAutoSyncUseCase(action.enabled)
            is Action.OnIntervalSelected -> setSyncIntervalUseCase(action.interval)
            is Action.OnNotificationChanged -> setNotificationEnabledUseCase(action.kind, action.enabled)
            Action.OnLogout -> logoutUseCase()
        }
    }

    data class State(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val sync: SyncSettings = SyncSettings(),
        val notifications: NotificationSettings = NotificationSettings(),
        /** Null until the profile is loaded, or if Tesla didn't give it. */
        val account: TeslaAccount? = null,
    ) : BaseState

    sealed class Action : BaseAction {
        data class OnThemeSelected(val mode: ThemeMode) : Action()
        data class OnAutoSyncChanged(val enabled: Boolean) : Action()
        data class OnIntervalSelected(val interval: SyncInterval) : Action()
        data class OnNotificationChanged(val kind: NotificationKind, val enabled: Boolean) : Action()
        data object OnLogout : Action()
    }
}
