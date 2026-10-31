package app.tostrr.domain.sync

import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.sync.usecase.SetAutoSyncUseCase
import app.tostrr.domain.sync.usecase.SetSyncIntervalUseCase
import app.tostrr.domain.sync.usecase.StartBackgroundSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSettingsTest {
    private class FakeSettings : SyncSettingsRepository {
        override val settings = MutableStateFlow(SyncSettings())
        override fun save(settings: SyncSettings) {
            this.settings.value = settings
        }
    }

    /** Records each call: an interval when scheduled, null when cancelled. */
    private class FakeScheduler : SyncScheduler {
        val calls = mutableListOf<SyncInterval?>()
        override fun schedule(interval: SyncInterval) {
            calls += interval
        }
        override fun cancel() {
            calls += null
        }
    }

    private class FakeAuth(loggedIn: Boolean) : AuthRepository {
        override fun startLogin() = ""
        override suspend fun completeLogin(redirect: String) = Unit
        override val loggedIn = MutableStateFlow(loggedIn)
        override fun logout() = Unit
    }

    private val settings = FakeSettings()
    private val scheduler = FakeScheduler()
    private fun start(loggedIn: Boolean = true) = StartBackgroundSyncUseCase(FakeAuth(loggedIn), scheduler, settings)
    private fun setInterval(loggedIn: Boolean = true) = SetSyncIntervalUseCase(settings, start(loggedIn))
    private fun setAutoSync() = SetAutoSyncUseCase(settings, start())

    @Test fun `no interval is below the WorkManager minimum`() {
        assertEquals(15L, SyncInterval.entries.minOf { it.minutes })
    }

    @Test fun `a stored value no longer offered falls back to the default`() {
        assertEquals(SyncInterval.HOUR_1, SyncInterval.fromMinutes(60))
        assertEquals(SyncInterval.DEFAULT, SyncInterval.fromMinutes(45))
    }

    @Test fun `a new interval is saved and rescheduled`() {
        setInterval()(SyncInterval.HOURS_2)
        assertEquals(SyncInterval.HOURS_2, settings.settings.value.interval)
        assertEquals(listOf(SyncInterval.HOURS_2), scheduler.calls)
    }

    @Test fun `the same interval changes nothing`() {
        setInterval()(SyncInterval.DEFAULT)
        assertEquals(emptyList<SyncInterval?>(), scheduler.calls)
    }

    @Test fun `logged out, the interval is saved but nothing is scheduled`() {
        setInterval(loggedIn = false)(SyncInterval.HOURS_6)
        assertEquals(SyncInterval.HOURS_6, settings.settings.value.interval)
        assertEquals(listOf<SyncInterval?>(null), scheduler.calls)
    }

    @Test fun `turning auto sync off cancels it and on schedules it back at the saved interval`() {
        settings.save(SyncSettings(interval = SyncInterval.HOUR_1))
        setAutoSync()(false)
        assertEquals(false, settings.settings.value.autoSync)
        setAutoSync()(true)
        assertEquals(listOf(null, SyncInterval.HOUR_1), scheduler.calls)
    }

    @Test fun `with auto sync off, startup and a new interval keep it cancelled`() {
        settings.save(SyncSettings(autoSync = false))
        start()()
        setInterval()(SyncInterval.HOURS_2)
        assertEquals(listOf<SyncInterval?>(null, null), scheduler.calls)
        assertEquals(SyncInterval.HOURS_2, settings.settings.value.interval)
    }
}
