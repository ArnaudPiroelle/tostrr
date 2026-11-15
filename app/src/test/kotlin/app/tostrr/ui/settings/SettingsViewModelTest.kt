package app.tostrr.ui.settings

import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.notification.NotificationKind
import app.tostrr.domain.notification.NotificationSettings
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.testing.MainDispatcherRule
import app.tostrr.testing.TestGraph
import app.tostrr.domain.update.UpdateCheck
import app.tostrr.testing.order
import app.tostrr.testing.sideEffects
import app.tostrr.testing.watch
import app.tostrr.ui.settings.SettingsViewModel.Action
import app.tostrr.ui.settings.SettingsViewModel.SideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val graph = TestGraph()

    @Test fun `the saved settings and account show at once`() {
        graph.syncSettings.settings.value = SyncSettings(autoSync = false, interval = SyncInterval.HOURS_2)
        graph.account.account.value = TeslaAccount("Ada Lovelace")
        graph.appearance.themeMode.value = ThemeMode.DARK

        val state = graph.settingsViewModel().state.value

        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(SyncSettings(autoSync = false, interval = SyncInterval.HOURS_2), state.sync)
        assertEquals(TeslaAccount("Ada Lovelace"), state.account)
    }

    @Test fun `the account is refreshed once the screen watches it`() = runTest {
        graph.account.account.value = TeslaAccount("Old name")
        graph.account.fetched = TeslaAccount("New name", avatarPath = "/avatar")
        val viewModel = graph.settingsViewModel()

        watch(viewModel)

        assertEquals(1, graph.account.loads)
        assertEquals(TeslaAccount("New name", avatarPath = "/avatar"), viewModel.state.value.account)
    }

    @Test fun `a picked theme is saved and shown`() = runTest {
        val viewModel = graph.settingsViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnThemeSelected(ThemeMode.LIGHT))

        assertEquals(ThemeMode.LIGHT, graph.appearance.themeMode.value)
        assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)
    }

    @Test fun `turning auto sync off saves it and cancels the background sync`() = runTest {
        val viewModel = graph.settingsViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnAutoSyncChanged(false))

        assertFalse(viewModel.state.value.sync.autoSync)
        assertEquals(listOf<SyncInterval?>(null), graph.scheduler.calls)
    }

    @Test fun `a new interval is saved and rescheduled`() = runTest {
        val viewModel = graph.settingsViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnIntervalSelected(SyncInterval.HOUR_1))

        assertEquals(SyncInterval.HOUR_1, viewModel.state.value.sync.interval)
        assertEquals(listOf<SyncInterval?>(SyncInterval.HOUR_1), graph.scheduler.calls)
    }

    @Test fun `a notification kind turned off is saved and shown`() = runTest {
        val viewModel = graph.settingsViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnNotificationChanged(NotificationKind.DIGEST, false))

        assertEquals(NotificationSettings(setOf(NotificationKind.DIGEST)), graph.notificationSettings.settings.value)
        assertFalse(viewModel.state.value.notifications.isEnabled(NotificationKind.DIGEST))
    }

    @Test fun `logging out ends the session and forgets the orders and the account`() = runTest {
        graph.orders.orders.value = listOf(order())
        graph.account.account.value = TeslaAccount("Ada Lovelace")
        val viewModel = graph.settingsViewModel()
        watch(viewModel)

        viewModel.handle(Action.OnLogout)

        assertFalse(graph.auth.loggedIn.value)
        assertEquals(emptyList<Any>(), graph.orders.orders.value)
        assertNull(viewModel.state.value.account)
        assertEquals(listOf<SyncInterval?>(null), graph.scheduler.calls)
    }

    @Test fun `checking for an update shows it is running, then that the app is up to date`() = runTest {
        graph.updates.checking = CompletableDeferred()
        val viewModel = graph.settingsViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnCheckForUpdate)
        assertTrue(viewModel.state.value.checkingUpdate)
        viewModel.handle(Action.OnCheckForUpdate)
        graph.updates.checking?.complete(Unit)

        assertFalse(viewModel.state.value.checkingUpdate)
        assertEquals(UpdateCheck.UP_TO_DATE, viewModel.state.value.updateCheck)
        assertEquals(1, graph.updates.checksNow)
        assertTrue(effects.isEmpty())
    }

    @Test fun `a check that finds an update shows it and opens it`() = runTest {
        graph.updates.checkResult = UpdateCheck.AVAILABLE
        val viewModel = graph.settingsViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnCheckForUpdate)

        assertEquals(UpdateCheck.AVAILABLE, viewModel.state.value.updateCheck)
        assertEquals(graph.updates.found, viewModel.state.value.update)
        assertEquals(listOf(SideEffect.OpenUpdate), effects)
    }

    @Test fun `a check that fails says so and can be tried again`() = runTest {
        graph.updates.checkResult = UpdateCheck.FAILED
        val viewModel = graph.settingsViewModel()
        watch(viewModel)
        val effects = sideEffects(viewModel)

        viewModel.handle(Action.OnCheckForUpdate)
        assertEquals(UpdateCheck.FAILED, viewModel.state.value.updateCheck)
        assertNull(viewModel.state.value.update)

        graph.updates.checkResult = UpdateCheck.UP_TO_DATE
        viewModel.handle(Action.OnCheckForUpdate)
        assertEquals(UpdateCheck.UP_TO_DATE, viewModel.state.value.updateCheck)
        assertEquals(2, graph.updates.checksNow)
        assertTrue(effects.isEmpty())
    }
}
