package app.tostrr.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import app.tostrr.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import app.tostrr.domain.order.Order
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.orders.OrdersViewModel.Action
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import app.tostrr.ui.common.StepTracker
import app.tostrr.ui.common.formatDateTime
import app.tostrr.ui.theme.TostrrColors

private const val TOOLTIP_VISIBLE_MS = 2_500L
private const val CARD_IMAGE_ZOOM = 1.8f

/** Places a tooltip under its anchor, right-aligned with it, keeping a margin to the screen edge. */
@Composable
private fun rememberBelowAnchorPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density) {
        val gap = with(density) { 4.dp.roundToPx() }
        val edge = with(density) { 12.dp.roundToPx() }
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val maxX = (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge)
                val x = (anchorBounds.right - popupContentSize.width).coerceIn(edge, maxX)
                return IntOffset(x, anchorBounds.bottom + gap)
            }
        }
    }
}

@Composable
fun OrdersScreen(
    onOpen: (Order) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.watchAsState()
    OrdersContent(
        orders = state.orders,
        refreshing = state.sync.refreshing,
        lastRefreshAtMs = state.sync.lastRefreshAtMs,
        error = state.sync.error,
        onRefresh = { viewModel.handle(Action.OnRefresh) },
        onOpen = onOpen,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrdersContent(
    orders: List<Order>,
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
                        Text("Tostrr", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    TooltipBox(
                        positionProvider = rememberBelowAnchorPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text("Actualisation trop récente. Réessayez dans $remainingSec s.")
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
                                    contentDescription = "Actualiser",
                                    tint = if (coolingDown) TostrrColors.Muted.copy(alpha = 0.5f) else TostrrColors.OnSurface,
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Réglages")
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
                if (orders.isEmpty() && !refreshing) {
                    item {
                        Text(
                            "Aucune commande en cours.",
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

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TostrrColors.Surface),
        border = BorderStroke(0.5.dp, TostrrColors.Border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            "Commande",
                            Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TostrrColors.Muted,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "#${order.reference}",
                            Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(order.modelName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "VIN: ${order.vin ?: "inconnu"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TostrrColors.Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    order.deliverySlot?.let { slot ->
                        Text(
                            "Créneau : $slot",
                            style = MaterialTheme.typography.labelMedium,
                            color = TostrrColors.Muted,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = order.imageUrl,
                    contentDescription = order.modelName,
                    // Layout size is unchanged; the zoom only affects drawing. The image's transparent margins
                    // absorb the overflow, so the car grows without pushing any other content around.
                    modifier = Modifier.width(140.dp).height(76.dp).graphicsLayer(scaleX = CARD_IMAGE_ZOOM, scaleY = CARD_IMAGE_ZOOM),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(16.dp))
            StepTracker(order)
            Text(
                "Mise à jour ${formatDateTime(order.fetchedAtEpochMs)}",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TostrrColors.Muted,
                textAlign = TextAlign.End,
            )
        }
    }
}
