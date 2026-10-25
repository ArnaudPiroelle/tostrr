package com.tostrr.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tostrr.data.ApiException
import com.tostrr.data.AuthException
import com.tostrr.data.ChecklistRepository
import com.tostrr.data.DeliveryCenterCatalog
import com.tostrr.data.OptionCatalog
import com.tostrr.data.OrderRepository
import com.tostrr.data.RefreshScheduler
import com.tostrr.data.SessionExpiredException
import com.tostrr.data.TeslaAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** Minimum delay between two refreshes, to avoid hammering Tesla's private API. */
const val REFRESH_COOLDOWN_MS = 30_000L

@KoinViewModel
class MainViewModel(
    private val auth: TeslaAuth,
    private val scheduler: RefreshScheduler,
    val repository: OrderRepository,
    catalog: OptionCatalog,
    val checklists: ChecklistRepository,
    val deliveryCenters: DeliveryCenterCatalog,
) : ViewModel() {
    val optionCodes = catalog.codes

    private val _loggedIn = MutableStateFlow(auth.isLoggedIn())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Epoch ms of the last refresh attempt; 0 when none yet. Drives the refresh cooldown. */
    private val _lastRefreshAt = MutableStateFlow(0L)
    val lastRefreshAt: StateFlow<Long> = _lastRefreshAt.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (_loggedIn.value) refresh()
    }

    fun startLogin(): Uri {
        _error.value = null
        return auth.startLogin()
    }

    /** Called with the `tesla://auth/callback?...` URI, from the deep link or a pasted URL. */
    fun handleRedirect(redirect: String) {
        // A redirect delivered twice (deep link, then a paste) would fail: its code was already exchanged.
        if (_loggedIn.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                auth.completeLogin(redirect)
                scheduler.schedule()
                _loggedIn.value = true
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = "Connexion impossible : ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (_refreshing.value || now - _lastRefreshAt.value < REFRESH_COOLDOWN_MS) return
        _lastRefreshAt.value = now
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SessionExpiredException) {
                logout()
                _error.value = e.message
            } catch (e: Exception) {
                // Auth and API errors carry a message meant for the user; anything else gets context.
                _error.value = if (e is AuthException || e is ApiException) e.message else "Synchronisation impossible : ${e.message}"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun logout() {
        scheduler.cancel()
        auth.logout()
        repository.clear()
        _loggedIn.value = false
    }

    fun dismissError() {
        _error.value = null
    }
}
