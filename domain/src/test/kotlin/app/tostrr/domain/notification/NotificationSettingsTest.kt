package app.tostrr.domain.notification

import app.tostrr.domain.notification.usecase.SetNotificationEnabledUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsTest {
    private class FakeSettings : NotificationSettingsRepository {
        override val settings = MutableStateFlow(NotificationSettings())
        var saves = 0
        override fun save(settings: NotificationSettings) {
            saves++
            this.settings.value = settings
        }
    }

    private val repository = FakeSettings()
    private val setEnabled = SetNotificationEnabledUseCase(repository)

    @Test fun `every kind is on by default`() {
        assertTrue(NotificationKind.entries.all { NotificationSettings().isEnabled(it) })
    }

    @Test fun `a kind turned off stays off alone, then back on`() {
        setEnabled(NotificationKind.DIGEST, false)
        assertFalse(repository.settings.value.isEnabled(NotificationKind.DIGEST))
        assertTrue(repository.settings.value.isEnabled(NotificationKind.PROGRESS))

        setEnabled(NotificationKind.DIGEST, true)
        assertEquals(NotificationSettings(), repository.settings.value)
    }

    @Test fun `the same value saves nothing`() {
        setEnabled(NotificationKind.ALERTS, true)
        assertEquals(0, repository.saves)
    }
}
