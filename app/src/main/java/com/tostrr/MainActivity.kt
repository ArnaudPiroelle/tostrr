package com.tostrr

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
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tostrr.ui.MainViewModel
import com.tostrr.ui.checklist.ChecklistScreen
import com.tostrr.ui.detail.DetailScreen
import com.tostrr.ui.login.LoginScreen
import com.tostrr.ui.orders.OrdersScreen
import com.tostrr.ui.theme.TostrrTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** Order to open as soon as it is available, set by a tap on a home-screen widget. */
    private var pendingOrderRef by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default style: status/navigation bar icons switch between light and dark with the system theme.
        enableEdgeToEdge()
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            TostrrTheme {
                val nav = rememberNavController()
                val loggedIn by viewModel.loggedIn.collectAsState()
                val busy by viewModel.busy.collectAsState()
                val refreshing by viewModel.refreshing.collectAsState()
                val lastRefreshAt by viewModel.lastRefreshAt.collectAsState()
                val error by viewModel.error.collectAsState()
                val orders by viewModel.repository.orders.collectAsState()
                val history by viewModel.repository.history.collectAsState()
                val optionCodes by viewModel.optionCodes.collectAsState()

                LaunchedEffect(pendingOrderRef, loggedIn, orders) {
                    val ref = pendingOrderRef ?: return@LaunchedEffect
                    if (!loggedIn || orders.isEmpty()) return@LaunchedEffect
                    if (orders.any { it.reference == ref }) nav.navigate("order/$ref") { launchSingleTop = true }
                    pendingOrderRef = null
                }

                LaunchedEffect(loggedIn) {
                    if (loggedIn) {
                        requestNotificationPermission()
                        requestBatteryOptimizationExemption()
                    }
                    val route = nav.currentDestination?.route
                    if (loggedIn && route == "login") nav.navigate("orders") { popUpTo("login") { inclusive = true } }
                    if (!loggedIn && route != null && route != "login") nav.navigate("login") { popUpTo(0) }
                }

                NavHost(nav, startDestination = if (loggedIn) "orders" else "login") {
                    composable("login") {
                        LoginScreen(
                            busy = busy,
                            error = error,
                            onLogin = { openLogin() },
                            onPasteRedirect = viewModel::handleRedirect,
                        )
                    }
                    composable("orders") {
                        OrdersScreen(
                            orders = orders,
                            refreshing = refreshing,
                            lastRefreshAtMs = lastRefreshAt,
                            error = error,
                            onRefresh = viewModel::refresh,
                            onOpen = { nav.navigate("order/${it.reference}") },
                            onLogout = viewModel::logout,
                        )
                    }
                    composable("order/{ref}") { entry ->
                        val order = entry.arguments?.getString("ref")?.let { ref -> orders.firstOrNull { it.reference == ref } }
                        if (order != null) {
                            val checklist by remember(order.reference) { viewModel.checklists.observe(order.reference) }
                                .collectAsState(null)
                            DetailScreen(
                                order,
                                history[order.reference].orEmpty(),
                                optionCodes,
                                checklist = checklist,
                                onOpenChecklist = { nav.navigate("order/${order.reference}/checklist") },
                                onBack = { nav.popBackStack() },
                            )
                        }
                    }
                    composable("order/{ref}/checklist") {
                        ChecklistScreen(onClose = { nav.popBackStack() })
                    }
                }
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
        if (data.scheme == WIDGET_SCHEME && data.host == "order") pendingOrderRef = data.pathSegments.firstOrNull()
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
        /** Internal link used by the widget: `tostrr://order/<reference>`. */
        const val WIDGET_SCHEME = "tostrr"

        private const val KEY_BATTERY_PROMPT = "battery_prompt_shown"
    }
}
