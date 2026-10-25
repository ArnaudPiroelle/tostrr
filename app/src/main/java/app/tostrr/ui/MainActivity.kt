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
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.ui.checklist.ChecklistScreen
import app.tostrr.ui.detail.DetailScreen
import app.tostrr.ui.login.LoginScreen
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.orders.OrdersScreen
import app.tostrr.ui.theme.TostrrTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    /** Order to open as soon as it is available, set by a tap on a home-screen widget or a launcher shortcut. */
    private var pendingOrderRef by mutableStateOf<String?>(null)

    /** Set by the "Commandes" launcher shortcut: go back to the orders list, wherever the app was. */
    private var pendingOrdersList by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default style: status/navigation bar icons switch between light and dark with the system theme.
        enableEdgeToEdge()
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            TostrrTheme {
                val loggedIn by viewModel.loggedIn.collectAsState()
                val backStack = rememberNavBackStack(if (loggedIn) Screen.Orders else Screen.Login)
                val busy by viewModel.busy.collectAsState()
                val refreshing by viewModel.refreshing.collectAsState()
                val lastRefreshAt by viewModel.lastRefreshAt.collectAsState()
                val error by viewModel.error.collectAsState()
                val orders by viewModel.orders.collectAsState()
                val history by viewModel.history.collectAsState()
                val optionCodes by viewModel.optionCodes.collectAsState()

                LaunchedEffect(pendingOrderRef, loggedIn, orders) {
                    val ref = pendingOrderRef ?: return@LaunchedEffect
                    if (!loggedIn || orders.isEmpty()) return@LaunchedEffect
                    val target = Screen.OrderDetail(ref)
                    if (orders.any { it.reference == ref } && backStack.lastOrNull() != target) backStack.add(target)
                    pendingOrderRef = null
                }

                LaunchedEffect(pendingOrdersList, loggedIn) {
                    if (!pendingOrdersList || !loggedIn) return@LaunchedEffect
                    while (backStack.size > 1 && backStack.last() != Screen.Orders) backStack.removeAt(backStack.lastIndex)
                    pendingOrdersList = false
                }

                LaunchedEffect(loggedIn) {
                    if (loggedIn) {
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

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    // Each entry keeps its own saved state and ViewModels, cleared when it leaves the stack.
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider {
                        entry<Screen.Login> {
                            LoginScreen(
                                busy = busy,
                                error = error,
                                onLogin = { openLogin() },
                                onPasteRedirect = viewModel::handleRedirect,
                            )
                        }
                        entry<Screen.Orders> {
                            OrdersScreen(
                                orders = orders,
                                refreshing = refreshing,
                                lastRefreshAtMs = lastRefreshAt,
                                error = error,
                                onRefresh = viewModel::refresh,
                                onOpen = { backStack.add(Screen.OrderDetail(it.reference)) },
                                onLogout = viewModel::logout,
                            )
                        }
                        entry<Screen.OrderDetail> { key ->
                            val order = orders.firstOrNull { it.reference == key.reference }
                            if (order != null) {
                                val checklist by remember(order.reference) { viewModel.checklist(order.reference) }
                                    .collectAsState(null)
                                val deliveryCenter by produceState<DeliveryCenter?>(null, order.deliveryCenter, order.countryCode) {
                                    value = viewModel.deliveryCenter(order)
                                }
                                DetailScreen(
                                    order,
                                    history[order.reference].orEmpty(),
                                    optionCodes,
                                    checklist = checklist,
                                    deliveryCenter = deliveryCenter,
                                    onOpenChecklist = { backStack.add(Screen.Checklist(order.reference)) },
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }
                        }
                        entry<Screen.Checklist> { key ->
                            ChecklistScreen(orderReference = key.reference, onClose = { backStack.removeLastOrNull() })
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
        if (data.scheme == "tesla" && data.host == "auth") viewModel.handleRedirect(data.toString())
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
     * Asks once, with the system dialog, to exempt the app from battery optimisation: otherwise the 15-minute background
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

    private fun openLogin() {
        val uri = viewModel.startLogin()
        CustomTabsIntent.Builder().build().launchUrl(this, uri)
    }

    companion object {
        /** Internal links used by the widgets and launcher shortcuts: `tostrr://orders` and `tostrr://order/<reference>`. */
        const val DEEP_LINK_SCHEME = "tostrr"

        /** Opens the order's detail screen. */
        fun orderUri(reference: String): Uri = Uri.parse("$DEEP_LINK_SCHEME://order/$reference")

        private const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
