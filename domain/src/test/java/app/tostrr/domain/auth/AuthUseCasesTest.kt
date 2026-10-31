package app.tostrr.domain.auth

import app.tostrr.domain.auth.usecase.CompleteLoginUseCase
import app.tostrr.domain.auth.usecase.LoadTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderRepository
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncScheduler
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.domain.sync.SyncSettingsRepository
import app.tostrr.domain.sync.usecase.StartBackgroundSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {
    /** Every call made on the fakes, in order, across them all. */
    private val calls = mutableListOf<String>()

    private inner class FakeAuth(loggedIn: Boolean, private val failure: AuthException? = null) : AuthRepository {
        override fun startLogin() = "https://auth"
        override suspend fun completeLogin(redirect: String) {
            failure?.let { throw it }
            calls += "completeLogin $redirect"
            loggedIn.value = true
        }
        override val loggedIn = MutableStateFlow(loggedIn)
        override fun logout() {
            calls += "logout"
            loggedIn.value = false
        }
    }

    private inner class FakeScheduler : SyncScheduler {
        override fun schedule(interval: SyncInterval) {
            calls += "schedule $interval"
        }
        override fun cancel() {
            calls += "cancel"
        }
    }

    private class FakeSettings : SyncSettingsRepository {
        override val settings = MutableStateFlow(SyncSettings())
        override fun save(settings: SyncSettings) {
            this.settings.value = settings
        }
    }

    private inner class FakeOrders : OrderRepository {
        override val orders = MutableStateFlow(emptyList<Order>())
        override val history = MutableStateFlow(emptyMap<String, List<Change>>())
        override val syncing = MutableStateFlow(false)
        override fun lastSyncAgeMs(now: Long): Long? = null
        override suspend fun refresh() = Unit
        override fun clear() {
            calls += "clear orders"
        }
    }

    private inner class FakeAccount : AccountRepository {
        override val account = MutableStateFlow<TeslaAccount?>(TeslaAccount("Jane"))
        override suspend fun load() {
            calls += "load account"
        }
        override fun clear() {
            calls += "clear account"
        }
    }

    private val scheduler = FakeScheduler()
    private fun startSync(auth: AuthRepository) = StartBackgroundSyncUseCase(auth, scheduler, FakeSettings())

    @Test fun `a completed login starts the background sync`() = runBlocking {
        val auth = FakeAuth(loggedIn = false)
        CompleteLoginUseCase(auth, startSync(auth))("tesla://auth/callback?code=1")
        assertEquals(listOf("completeLogin tesla://auth/callback?code=1", "schedule ${SyncInterval.DEFAULT}"), calls)
    }

    @Test fun `a failed login schedules nothing`() = runBlocking {
        val auth = FakeAuth(loggedIn = false, failure = AuthException(AuthError.INVALID_STATE))
        val error = runCatching { CompleteLoginUseCase(auth, startSync(auth))("tesla://auth/callback") }.exceptionOrNull()
        assertEquals(AuthError.INVALID_STATE, (error as AuthException).reason)
        assertTrue(calls.isEmpty())
    }

    @Test fun `the account is only fetched while logged in`() = runBlocking {
        LoadTeslaAccountUseCase(FakeAuth(loggedIn = false), FakeAccount())()
        assertTrue(calls.isEmpty())
        LoadTeslaAccountUseCase(FakeAuth(loggedIn = true), FakeAccount())()
        assertEquals(listOf("load account"), calls)
    }

    @Test fun `logging out stops the sync before dropping the session, then forgets the orders and the account`() {
        val auth = FakeAuth(loggedIn = true)
        LogoutUseCase(auth, scheduler, FakeOrders(), FakeAccount())()
        assertEquals(listOf("cancel", "logout", "clear orders", "clear account"), calls)
        assertEquals(false, auth.isLoggedIn())
    }
}
