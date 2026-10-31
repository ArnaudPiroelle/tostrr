package app.tostrr.ui.orders.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Order
import app.tostrr.ui.orders.REFRESH_COOLDOWN_MS
import app.tostrr.ui.theme.TostrrColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TOOLTIP_VISIBLE_MS = 2_500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrdersContent(
    orders: List<Order>,
    loading: Boolean,
    refreshing: Boolean,
    lastRefreshAtMs: Long,
    error: String?,
    onRefresh: () -> Unit,
    onOpen: (Order) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)

    // Ticks once per second while the cooldown is running so the label and button state stay current.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastRefreshAtMs) {
        while (true) {
            now = System.currentTimeMillis()
            if (now - lastRefreshAtMs >= REFRESH_COOLDOWN_MS) break
            delay(1_000)
        }
    }
    val remainingSec = ((lastRefreshAtMs + REFRESH_COOLDOWN_MS - now + 999) / 1000).coerceAtLeast(0)
    val coolingDown = remainingSec > 0

    // Too soon -> explain with the tooltip instead of refreshing.
    val requestRefresh: () -> Unit = {
        if (coolingDown) {
            // show() suspends until the persistent tooltip is dismissed, so the timer must run alongside it.
            scope.launch {
                launch { tooltipState.show() }
                delay(TOOLTIP_VISIBLE_MS)
                tooltipState.dismiss()
            }
        } else {
            onRefresh()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_logo), contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    TooltipBox(
                        positionProvider = rememberBelowAnchorPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(R.string.orders_refresh_too_soon, remainingSec.toInt()))
                            }
                        },
                        state = tooltipState,
                    ) {
                        IconButton(onClick = requestRefresh) {
                            if (refreshing) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TostrrColors.Muted)
                            } else {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.orders_refresh),
                                    tint = if (coolingDown) TostrrColors.Muted.copy(alpha = 0.5f) else TostrrColors.OnSurface,
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.orders_settings))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (error != null) {
                    item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }
                if (loading) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(color = TostrrColors.Accent)
                            Text(stringResource(R.string.orders_loading), color = TostrrColors.Muted, textAlign = TextAlign.Center)
                        }
                    }
                } else if (orders.isEmpty() && !refreshing) {
                    item {
                        Text(
                            stringResource(R.string.orders_empty),
                            Modifier.fillMaxWidth().padding(top = 48.dp),
                            color = TostrrColors.Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(orders, key = { it.reference }) { order -> OrderCard(order, onClick = { onOpen(order) }) }
            }
        }
    }
}
