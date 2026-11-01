package app.tostrr.ui.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.common.uiErrorText
import app.tostrr.ui.orders.OrdersViewModel.Action
import app.tostrr.ui.orders.components.OrdersContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun OrdersScreen(
    onOpen: (Order) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.watchAsState()
    OrdersContent(
        orders = state.orders,
        loading = state.loading,
        refreshing = state.refreshing,
        lastRefreshAtMs = state.sync.lastRefreshAtMs,
        error = state.sync.error?.let { uiErrorText(it, R.string.error_sync_failed) },
        onRefresh = { viewModel.handle(Action.OnRefresh) },
        onOpen = onOpen,
        onOpenSettings = onOpenSettings,
    )
}
