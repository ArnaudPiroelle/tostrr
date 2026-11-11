package app.tostrr.data.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.OptionRepository
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderStep
import app.tostrr.domain.order.StepKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class OrderNotifierTest {
    private val context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private class FakeOptions : OptionRepository {
        override val codes = MutableStateFlow(mapOf("PPSW" to OptionInfo("Pearl White", "paint")))
    }

    private val settings = NotificationSettingsRepositoryImpl(context)
    private val notifier = OrderNotifier(context, FakeOptions(), settings)

    @Before fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // The notifications open whatever the launcher opens, which the library alone doesn't have.
        val launcher = ComponentName(context.packageName, "Launcher")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(launcher)
            addIntentFilterForActivity(launcher, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) })
        }
    }

    private fun order(
        vararg done: StepKind,
        options: List<String> = emptyList(),
        window: String? = null,
        appointmentAt: Long? = null,
    ) = Order(
        reference = "RN1", status = "BOOKED", vin = null, modelName = "Model Y", optionCodes = options, imageUrl = null,
        steps = StepKind.entries.map { OrderStep(it, null, it in done) }, deliveryCenter = null, deliveryWindow = window,
        deliverySlot = null, appointment = null, etaToDeliveryCenter = null, expectedRegistration = null,
        fetchedAtEpochMs = 0L, raw = JsonObject(emptyMap()), appointmentAtEpochMs = appointmentAt,
    )

    private val posted get() = shadowOf(manager).allNotifications
    private val titles get() = posted.map { it.extras.getString(Notification.EXTRA_TITLE) }
    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
    private fun Notification.bigText() = extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    /** Noon today on the device clock, so the digest's "after 9:00" holds. */
    private val noon = LocalDate.now().atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `a new order announces every step already done`() {
        assertTrue(notifier.notifyStepChanges(null, order(StepKind.ORDERED, StepKind.VIN_ASSIGNED)))
        assertEquals(setOf("Your Tesla order is confirmed", "Your Tesla now has a VIN"), titles.toSet())
        assertEquals("Model Y · RN1", posted.first().text())
    }

    @Test fun `only the steps newly done are announced`() {
        notifier.notifyStepChanges(order(StepKind.ORDERED), order(StepKind.ORDERED, StepKind.BUILT))
        assertEquals(listOf("Your Tesla has left the factory"), titles)
    }

    @Test fun `a delivery without appointment says delivered, not appointment confirmed`() {
        val before = order(StepKind.ORDERED)
        val after = order(StepKind.ORDERED, StepKind.DELIVERY).copy(inAccountSince = LocalDate.of(2026, 12, 20))
        assertTrue(notifier.notifyStepChanges(before, after))
        assertEquals(listOf("Your Tesla is delivered"), titles)
    }

    @Test fun `nothing changed means nothing posted`() {
        assertFalse(notifier.notifyStepChanges(order(StepKind.ORDERED), order(StepKind.ORDERED)))
        assertTrue(posted.isEmpty())
    }

    @Test fun `steps no longer done are reported together`() {
        val before = order(StepKind.ORDERED, StepKind.VIN_ASSIGNED, StepKind.BUILT)
        assertTrue(notifier.notifyStepChanges(before, order(StepKind.ORDERED)))
        val regression = posted.single()
        assertEquals(
            "Your order went backwards: the “VIN assigned”, “Production finished” steps are no longer done",
            regression.extras.getString(Notification.EXTRA_TITLE),
        )
    }

    @Test fun `a configuration change lists what was added and removed, with the labels known`() {
        assertTrue(notifier.notifyStepChanges(order(options = listOf("WY19B")), order(options = listOf("\$PPSW"))))
        val config = posted.single()
        assertEquals("Your Tesla's configuration is changing", config.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Added: 1 · Removed: 1", config.text())
        assertEquals("Added:\n• Pearl White (PPSW)\n\nRemoved:\n• WY19B", config.bigText())
    }

    @Test fun `a new delivery window is announced with its dates`() {
        notifier.notifyStepChanges(order(), order(window = "29 Nov – 29 Déc"))
        assertEquals("29 Nov – 29 Déc", posted.single().text())
    }

    @Test fun `without the permission nothing is posted`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.notifyStepChanges(null, order(StepKind.ORDERED))
        assertTrue(posted.isEmpty())
    }

    @Test fun `the minor update lists the fields that moved`() {
        notifier.notifyMinorUpdate(order(), listOf(Change(0, "details.tasks.a", "1", "2"), Change(0, "details.tasks.b", null, "x")))
        val minor = posted.single()
        assertEquals("Minor update to your order", minor.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(minor.bigText(), "• details.tasks.a" in minor.bigText())
        assertTrue(minor.bigText(), "• details.tasks.b" in minor.bigText())
    }

    @Test fun `no minor update without changes`() {
        notifier.notifyMinorUpdate(order(), emptyList())
        assertTrue(posted.isEmpty())
    }

    @Test fun `a delivery reminder is sent once per appointment`() {
        val now = noon
        val o = order(appointmentAt = now + TimeUnit.HOURS.toMillis(12))
        notifier.notifyReminder(o, now)
        notifier.notifyReminder(o, now + TimeUnit.HOURS.toMillis(1))
        // A new notifier reads what was already sent.
        OrderNotifier(context, FakeOptions(), settings).notifyReminder(o, now + TimeUnit.HOURS.toMillis(2))
        assertEquals(listOf("Delivery in less than 24 h"), titles)
    }

    @Test fun `the first digest only starts counting`() {
        notifier.notifyDigest(mapOf("RN1" to listOf(Change(noon, "k", "a", "b"))), noon)
        assertTrue(posted.isEmpty())
    }

    @Test fun `the digest counts the changes since the last one, at most once a day`() {
        notifier.notifyDigest(emptyMap(), noon)
        val tomorrow = noon + TimeUnit.DAYS.toMillis(1)
        val history = mapOf(
            "RN1" to listOf(Change(noon + 1, "a", "1", "2"), Change(noon + 2, "b", "1", "2"), Change(noon - 1, "old", "1", "2")),
            "RN2" to listOf(Change(noon + 3, "c", "1", "2")),
        )
        notifier.notifyDigest(history, noon + TimeUnit.HOURS.toMillis(2))
        assertTrue(posted.isEmpty())
        notifier.notifyDigest(history, tomorrow)
        assertEquals(listOf("3 changes in 2 orders"), titles)
    }

    @Test fun `an expired session is reported, then cleared after a good sync`() {
        notifier.notifySyncProblem(expired = true, sinceMs = null)
        assertEquals(listOf("Tesla sign-in expired"), titles)
        notifier.clearSyncProblem()
        assertTrue(posted.isEmpty())
    }

    @Test fun `a stale sync says since when`() {
        notifier.notifySyncProblem(expired = false, sinceMs = TimeUnit.HOURS.toMillis(7))
        assertEquals("Last sync 7 h ago", posted.single().text())
        notifier.notifySyncProblem(expired = false, sinceMs = null)
        assertEquals("No sync has succeeded", posted.single().text())
    }

    @Test fun `a kind turned off is not posted, the others still are`() {
        settings.save(NotificationSettings(setOf(NotificationKind.PROGRESS)))
        notifier.notifyStepChanges(order(StepKind.ORDERED), order(StepKind.ORDERED, StepKind.BUILT))
        assertTrue(posted.isEmpty())
        notifier.notifySyncProblem(expired = true, sinceMs = null)
        assertEquals(listOf("Tesla sign-in expired"), titles)
    }

    @Test fun `a reminder turned off counts as sent, so turning it back on doesn't repeat it`() {
        val o = order(appointmentAt = noon + TimeUnit.HOURS.toMillis(12))
        settings.save(NotificationSettings(setOf(NotificationKind.REMINDERS)))
        notifier.notifyReminder(o, noon)
        settings.save(NotificationSettings())
        notifier.notifyReminder(o, noon + TimeUnit.HOURS.toMillis(1))
        assertTrue(posted.isEmpty())
    }
}
