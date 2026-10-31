package app.tostrr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.auth.usecase.LoadTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.ObserveTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.usecase.ObserveSyncSettingsUseCase
import app.tostrr.domain.sync.usecase.SetAutoSyncUseCase
import app.tostrr.domain.sync.usecase.SetSyncIntervalUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** The app settings, one group per section of the screen. */
@KoinViewModel
class SettingsViewModel(
    observeSyncSettingsUseCase: ObserveSyncSettingsUseCase,
    observeTeslaAccountUseCase: ObserveTeslaAccountUseCase,
    loadTeslaAccountUseCase: LoadTeslaAccountUseCase,
    private val setAutoSyncUseCase: SetAutoSyncUseCase,
    private val setSyncIntervalUseCase: SetSyncIntervalUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    val sync: StateFlow<SyncSettings> = observeSyncSettingsUseCase()

    /** Null until the profile is loaded, or if Tesla didn't give it. */
    val account: StateFlow<TeslaAccount?> = observeTeslaAccountUseCase()

    init {
        viewModelScope.launch { loadTeslaAccountUseCase() }
    }

    fun setAutoSync(enabled: Boolean) = setAutoSyncUseCase(enabled)

    fun setSyncInterval(interval: SyncInterval) = setSyncIntervalUseCase(interval)

    fun logout() = logoutUseCase()
}
