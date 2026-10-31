package app.tostrr.ui.settings

import androidx.lifecycle.ViewModel
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.usecase.ObserveSyncSettingsUseCase
import app.tostrr.domain.sync.usecase.SetAutoSyncUseCase
import app.tostrr.domain.sync.usecase.SetSyncIntervalUseCase
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.annotation.KoinViewModel

/** The app settings, one group per section of the screen. */
@KoinViewModel
class SettingsViewModel(
    observeSyncSettingsUseCase: ObserveSyncSettingsUseCase,
    private val setAutoSyncUseCase: SetAutoSyncUseCase,
    private val setSyncIntervalUseCase: SetSyncIntervalUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    val sync: StateFlow<SyncSettings> = observeSyncSettingsUseCase()

    fun setAutoSync(enabled: Boolean) = setAutoSyncUseCase(enabled)

    fun setSyncInterval(interval: SyncInterval) = setSyncIntervalUseCase(interval)

    fun logout() = logoutUseCase()
}
