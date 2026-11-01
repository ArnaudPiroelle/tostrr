package app.tostrr.testing

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.tostrr.domain.appearance.usecase.ObserveThemeModeUseCase
import app.tostrr.domain.appearance.usecase.SetThemeModeUseCase
import app.tostrr.domain.auth.usecase.CompleteLoginUseCase
import app.tostrr.domain.auth.usecase.LoadTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.LogoutUseCase
import app.tostrr.domain.auth.usecase.ObserveLoggedInUseCase
import app.tostrr.domain.auth.usecase.ObserveTeslaAccountUseCase
import app.tostrr.domain.auth.usecase.StartLoginUseCase
import app.tostrr.domain.checklist.usecase.AnswerChecklistQuestionUseCase
import app.tostrr.domain.checklist.usecase.CreateChecklistPhotoFileUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.DeleteChecklistUseCase
import app.tostrr.domain.checklist.usecase.ImportChecklistPhotoUseCase
import app.tostrr.domain.checklist.usecase.MoveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ObserveChecklistUseCase
import app.tostrr.domain.checklist.usecase.ReopenChecklistUseCase
import app.tostrr.domain.checklist.usecase.StartChecklistUseCase
import app.tostrr.domain.checklist.usecase.UpdateChecklistAnswerUseCase
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.deliverycenter.usecase.FindDeliveryCenterUseCase
import app.tostrr.domain.layout.usecase.MoveDetailBlockUseCase
import app.tostrr.domain.layout.usecase.ObserveDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ResetDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockExpandedUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockUseCase
import app.tostrr.domain.notification.usecase.ObserveNotificationSettingsUseCase
import app.tostrr.domain.notification.usecase.SetNotificationEnabledUseCase
import app.tostrr.domain.order.usecase.ObserveOptionCodesUseCase
import app.tostrr.domain.order.usecase.ObserveOrderHistoryUseCase
import app.tostrr.domain.order.usecase.ObserveOrdersSyncingUseCase
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.domain.order.usecase.RefreshOrdersUseCase
import app.tostrr.domain.sync.usecase.ObserveSyncSettingsUseCase
import app.tostrr.domain.sync.usecase.SetAutoSyncUseCase
import app.tostrr.domain.sync.usecase.SetSyncIntervalUseCase
import app.tostrr.domain.sync.usecase.StartBackgroundSyncUseCase
import app.tostrr.ui.MainViewModel
import app.tostrr.ui.checklist.ChecklistViewModel
import app.tostrr.ui.detail.DetailViewModel
import app.tostrr.ui.login.LoginViewModel
import app.tostrr.ui.orders.OrdersViewModel
import app.tostrr.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.nio.file.Files

/**
 * The app's real ViewModels and use cases, wired by hand on top of the fakes as Koin would: what a test changes in a
 * fake reaches the screen through the same use cases as in the app.
 */
class TestGraph(
    /** Where the first sync after login runs; a test scope makes it run before the test checks. */
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
) {
    val auth = FakeAuthRepository()
    val scheduler = FakeSyncScheduler()
    val syncSettings = FakeSyncSettingsRepository()
    val appearance = FakeAppearanceRepository()
    val notificationSettings = FakeNotificationSettingsRepository()
    val account = FakeAccountRepository()
    val orders = FakeOrderRepository()
    val options = FakeOptionRepository()
    val deliveryCenters = mutableListOf<DeliveryCenter>()
    val layout = FakeDetailLayoutRepository()
    val checklists = FakeChecklistRepository(Files.createTempDirectory("photos").toFile())

    private val startBackgroundSync get() = StartBackgroundSyncUseCase(auth, scheduler, syncSettings)
    private val logout get() = LogoutUseCase(auth, scheduler, orders, account)

    fun mainViewModel() = MainViewModel(ObserveLoggedInUseCase(auth), ObserveOrdersUseCase(orders), ObserveThemeModeUseCase(appearance))

    fun loginViewModel() = LoginViewModel(
        ObserveLoggedInUseCase(auth),
        StartLoginUseCase(auth),
        CompleteLoginUseCase(auth, startBackgroundSync),
        RefreshOrdersUseCase(orders),
        appScope,
    )

    fun ordersViewModel() = OrdersViewModel(
        ObserveOrdersUseCase(orders),
        ObserveOrdersSyncingUseCase(orders),
        RefreshOrdersUseCase(orders),
        logout,
    )

    fun detailViewModel(reference: String) = DetailViewModel(
        reference,
        ObserveOrdersUseCase(orders),
        ObserveOrderHistoryUseCase(orders),
        ObserveOptionCodesUseCase(options),
        ObserveChecklistUseCase(checklists),
        ObserveDetailLayoutUseCase(layout),
        FindDeliveryCenterUseCase(FakeDeliveryCenterRepository(deliveryCenters)),
        MoveDetailBlockUseCase(layout),
        ToggleDetailBlockUseCase(layout),
        ResetDetailLayoutUseCase(layout),
        ToggleDetailBlockExpandedUseCase(layout),
    )

    fun checklistViewModel(reference: String, context: Context, handle: SavedStateHandle = SavedStateHandle()) =
        ChecklistViewModel(
            reference,
            context,
            handle,
            ObserveChecklistUseCase(checklists),
            StartChecklistUseCase(checklists),
            MoveChecklistUseCase(checklists),
            ReopenChecklistUseCase(checklists),
            AnswerChecklistQuestionUseCase(checklists),
            UpdateChecklistAnswerUseCase(checklists),
            DeleteChecklistUseCase(checklists),
            CreateChecklistPhotoFileUseCase(checklists),
            ImportChecklistPhotoUseCase(checklists),
            DeleteChecklistPhotoUseCase(checklists),
        )

    fun settingsViewModel() = SettingsViewModel(
        ObserveThemeModeUseCase(appearance),
        ObserveSyncSettingsUseCase(syncSettings),
        ObserveNotificationSettingsUseCase(notificationSettings),
        ObserveTeslaAccountUseCase(account),
        LoadTeslaAccountUseCase(auth, account),
        SetThemeModeUseCase(appearance),
        SetAutoSyncUseCase(syncSettings, startBackgroundSync),
        SetSyncIntervalUseCase(syncSettings, startBackgroundSync),
        SetNotificationEnabledUseCase(notificationSettings),
        logout,
    )
}
