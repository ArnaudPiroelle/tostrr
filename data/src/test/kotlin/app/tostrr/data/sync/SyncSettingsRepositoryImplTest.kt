package app.tostrr.data.sync

import android.content.Context
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SyncSettingsRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)

    @Test fun `nothing saved means the defaults`() {
        assertEquals(SyncSettings(), SyncSettingsRepositoryImpl(context).settings.value)
    }

    @Test fun `saved settings are read back after a restart`() {
        val saved = SyncSettings(autoSync = false, interval = SyncInterval.HOURS_6)
        SyncSettingsRepositoryImpl(context).save(saved)
        assertEquals(saved, SyncSettingsRepositoryImpl(context).settings.value)
    }

    @Test fun `the defaults are stored as nothing at all`() {
        val repository = SyncSettingsRepositoryImpl(context)
        repository.save(SyncSettings(autoSync = false, interval = SyncInterval.HOUR_1))
        repository.save(SyncSettings())
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun `an interval no longer offered falls back to the default`() {
        prefs.edit().putLong("sync_interval_minutes", 45).commit()
        assertEquals(SyncInterval.DEFAULT, SyncSettingsRepositoryImpl(context).settings.value.interval)
    }
}
