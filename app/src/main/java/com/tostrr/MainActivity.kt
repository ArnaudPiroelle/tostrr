package com.tostrr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tostrr.ui.MainViewModel
import com.tostrr.ui.detail.DetailScreen
import com.tostrr.ui.login.LoginScreen
import com.tostrr.ui.orders.OrdersScreen
import com.tostrr.ui.theme.TostrrTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

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

                LaunchedEffect(loggedIn) {
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
                            DetailScreen(
                                order,
                                history[order.reference].orEmpty(),
                                optionCodes,
                                onBack = { nav.popBackStack() },
                            )
                        }
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
    }

    private fun openLogin() {
        val uri = viewModel.startLogin()
        CustomTabsIntent.Builder().build().launchUrl(this, uri)
    }
}
