package app.tostrr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.checklist.ChecklistScreen
import app.tostrr.ui.detail.DetailScreen
import app.tostrr.ui.login.LoginScreen
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.navigation.Transitions
import app.tostrr.ui.orders.OrdersScreen
import app.tostrr.ui.settings.SettingsScreen
import app.tostrr.ui.ship.ShipScreen
import app.tostrr.ui.theme.TostrrTheme
import app.tostrr.ui.theme.applySystemBars
import app.tostrr.ui.theme.isDark
import app.tostrr.ui.update.UpdateScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    /** Order to open as soon as it is available, set by a tap on a home-screen widget or a launcher shortcut. */
    private var pendingOrderRef by mutableStateOf<String?>(null)

    /** Set by the "Commandes" launcher shortcut: go back to the orders list, wherever the app was. */
    private var pendingOrdersList by mutableStateOf(false)

    /** The `tesla://auth/callback?...` URL Tesla redirected to, until the login screen takes it. */
    private var pendingRedirect by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(viewModel.state.value.themeMode.isDark(resources.configuration))
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            val state by viewModel.watchAsState()
            val dark = state.themeMode.isDark()

            TostrrTheme(darkTheme = dark) {
                val loggedIn = state.loggedIn
                val backStack = rememberNavBackStack(if (loggedIn) Screen.Orders else Screen.Login)

                // The ship screen's globe is dark in both themes: light status bar icons over it.
                val darkStatusBar = dark || backStack.lastOrNull() is Screen.Ship
                LaunchedEffect(dark, darkStatusBar) { applySystemBars(dark, darkStatusBar) }

                LaunchedEffect(pendingOrderRef, loggedIn, state.orderReferences) {
                    val ref = pendingOrderRef ?: return@LaunchedEffect
                    if (!loggedIn || state.orderReferences.isEmpty()) return@LaunchedEffect
                    val target = Screen.OrderDetail(ref)
                    if (ref in state.orderReferences && backStack.lastOrNull() != target) backStack.add(target)
                    pendingOrderRef = null
                }

                LaunchedEffect(pendingOrdersList, loggedIn) {
                    if (!pendingOrdersList || !loggedIn) return@LaunchedEffect
                    while (backStack.size > 1 && backStack.last() != Screen.Orders) backStack.removeAt(backStack.lastIndex)
                    pendingOrdersList = false
                }

                LaunchedEffect(loggedIn) {
                    if (loggedIn) {
                        // A redirect delivered again once logged in has nothing left to do.
                        pendingRedirect = null
                        requestNotificationPermission()
                        requestBatteryOptimizationExemption()
                    }
                    // Logging in or out replaces the whole stack, so back never returns to the other side.
                    val home = if (loggedIn) Screen.Orders else Screen.Login
                    if (backStack.firstOrNull() != home) {
                        backStack.clear()
                        backStack.add(home)
                    }
                }

                // Offered once per version from the orders list, so it never comes between a deep link and its screen.
                var offeredUpdate by rememberSaveable { mutableStateOf<String?>(null) }
                LaunchedEffect(state.availableUpdate, backStack.lastOrNull(), pendingOrderRef, pendingOrdersList) {
                    val version = state.availableUpdate ?: return@LaunchedEffect
                    if (version != offeredUpdate && backStack.lastOrNull() == Screen.Orders &&
                        pendingOrderRef == null && !pendingOrdersList
                    ) {
                        offeredUpdate = version
                        backStack.add(Screen.Update)
                    }
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = Transitions.push(),
                    popTransitionSpec = Transitions.pop(),
                    predictivePopTransitionSpec = Transitions.predictivePop(),
                    // Each entry keeps its own saved state and ViewModels, cleared when it leaves the stack.
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<Screen.Login> {
                            LoginScreen(redirect = pendingRedirect, onRedirectHandled = { pendingRedirect = null })
                        }
                        entry<Screen.Orders> {
                            OrdersScreen(
                                onOpen = { backStack.add(Screen.OrderDetail(it.reference)) },
                                onOpenSettings = { backStack.add(Screen.Settings) },
                            )
                        }
                        entry<Screen.OrderDetail> { key ->
                            DetailScreen(
                                reference = key.reference,
                                onOpenChecklist = { backStack.add(Screen.Checklist(key.reference)) },
                                onOpenShip = { backStack.add(Screen.Ship(key.reference, it)) },
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.Checklist> { key ->
                            ChecklistScreen(orderReference = key.reference, onClose = { backStack.removeLastOrNull() })
                        }
                        entry<Screen.Ship> { key ->
                            ShipScreen(key, onClose = { backStack.removeLastOrNull() })
                        }
                        entry<Screen.Settings> {
                            SettingsScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onOpenUpdate = { backStack.add(Screen.Update) },
                            )
                        }
                        entry<Screen.Update> {
                            UpdateScreen(onClose = { backStack.remove(Screen.Update) })
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "tesla" && data.host == "auth") pendingRedirect = data.toString()
        if (data.scheme == DEEP_LINK_SCHEME && data.host == "order") pendingOrderRef = data.pathSegments.firstOrNull()
        if (data.scheme == DEEP_LINK_SCHEME && data.host == "orders") pendingOrdersList = true
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Android 13+ needs a runtime grant; if refused, the system simply won't show the prompt again. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Asks once, with the system dialog, to exempt the app from battery optimisation: otherwise the periodic background
     * sync (hence the widget) can be deferred for hours. Not asked again after a refusal.
     */
    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val prefs = getSharedPreferences("tostrr_prompts", MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BATTERY_PROMPT, false) ||
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        ) return
        prefs.edit().putBoolean(KEY_BATTERY_PROMPT, true).apply()
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    companion object {
        /** Internal links used by the widgets and launcher shortcuts: `tostrr://orders` and `tostrr://order/<reference>`. */
        const val DEEP_LINK_SCHEME = "tostrr"

        /** Opens the order's detail screen. */
        fun orderUri(reference: String): Uri = Uri.parse("$DEEP_LINK_SCHEME://order/$reference")

        private const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
