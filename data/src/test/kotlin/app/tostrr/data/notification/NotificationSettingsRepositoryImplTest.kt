package app.tostrr.data.notification

import android.content.Context
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NotificationSettingsRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)

    @Test fun `every notification is on until told otherwise`() {
        assertEquals(NotificationSettings(), NotificationSettingsRepositoryImpl(context).settings.value)
    }

    @Test fun `the kinds turned off are read back after a restart`() {
        val off = NotificationSettings(setOf(NotificationKind.DIGEST, NotificationKind.MINOR_UPDATES))
        NotificationSettingsRepositoryImpl(context).save(off)
        assertEquals(off, NotificationSettingsRepositoryImpl(context).settings.value)
    }

    @Test fun `all on again stores nothing`() {
        val repository = NotificationSettingsRepositoryImpl(context)
        repository.save(NotificationSettings(setOf(NotificationKind.ALERTS)))
        repository.save(NotificationSettings())
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun `an unknown stored kind is ignored`() {
        prefs.edit().putStringSet("notifications_disabled", setOf("DIGEST", "GONE")).commit()
        assertEquals(NotificationSettings(setOf(NotificationKind.DIGEST)), NotificationSettingsRepositoryImpl(context).settings.value)
    }
}
