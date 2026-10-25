package app.tostrr.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tostrr.domain.auth.usecase.CompleteLoginUseCase
import app.tostrr.domain.auth.usecase.IsLoggedInUseCase
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.auth.usecase.StartLoginUseCase
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.usecase.ObserveChecklistUseCase
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.deliverycenter.usecase.FindDeliveryCenterUseCase
import app.tostrr.domain.error.ApiException
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.error.SessionExpiredException
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOptionCodesUseCase
import app.tostrr.domain.order.usecase.ObserveOrderHistoryUseCase
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.domain.order.usecase.RefreshOrdersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/** Minimum delay between two refreshes, to avoid hammering Tesla's private API. */
const val REFRESH_COOLDOWN_MS = 30_000L

@KoinViewModel
class MainViewModel(
    isLoggedInUseCase: IsLoggedInUseCase,
    private val startLoginUseCase: StartLoginUseCase,
    private val completeLoginUseCase: CompleteLoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshOrdersUseCase: RefreshOrdersUseCase,
    private val observeChecklistUseCase: ObserveChecklistUseCase,
    private val findDeliveryCenterUseCase: FindDeliveryCenterUseCase,
    observeOrdersUseCase: ObserveOrdersUseCase,
    observeOrderHistoryUseCase: ObserveOrderHistoryUseCase,
    observeOptionCodesUseCase: ObserveOptionCodesUseCase,
) : ViewModel() {
    val orders: StateFlow<List<Order>> = observeOrdersUseCase()
    val history: StateFlow<Map<String, List<Change>>> = observeOrderHistoryUseCase()
    val optionCodes: StateFlow<Map<String, OptionInfo>> = observeOptionCodesUseCase()

    private val _loggedIn = MutableStateFlow(isLoggedInUseCase())
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
        return Uri.parse(startLoginUseCase())
    }

    fun checklist(orderReference: String): Flow<Checklist?> = observeChecklistUseCase(orderReference)

    suspend fun deliveryCenter(order: Order): DeliveryCenter? = findDeliveryCenterUseCase(order.deliveryCenter, order.countryCode)

    /** Called with the `tesla://auth/callback?...` URI, from the deep link or a pasted URL. */
    fun handleRedirect(redirect: String) {
        // A redirect delivered twice (deep link, then a paste) would fail: its code was already exchanged.
        if (_loggedIn.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                completeLoginUseCase(redirect)
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
                refreshOrdersUseCase()
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
        logoutUseCase()
        _loggedIn.value = false
    }
}
