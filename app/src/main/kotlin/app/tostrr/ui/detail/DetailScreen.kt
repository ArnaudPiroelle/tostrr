package app.tostrr.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.common.StepTracker
import app.tostrr.ui.detail.DetailViewModel.Action
import app.tostrr.ui.detail.components.ChecklistCard
import app.tostrr.ui.detail.components.ConfigurationContent
import app.tostrr.ui.detail.components.DeliveryContent
import app.tostrr.ui.detail.components.DetailLayoutSheet
import app.tostrr.ui.detail.components.Hero
import app.tostrr.ui.detail.components.HistoryContent
import app.tostrr.ui.detail.components.PaymentContent
import app.tostrr.ui.detail.components.ProgressContent
import app.tostrr.ui.detail.components.Section
import app.tostrr.ui.detail.components.ShippingContent
import app.tostrr.ui.detail.components.StickyTopBar
import app.tostrr.ui.detail.components.VehicleContent
import app.tostrr.ui.detail.components.titleRes
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailScreen(
    reference: String,
    onOpenChecklist: () -> Unit,
    onOpenShip: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(reference) },
) {
    val state by viewModel.watchAsState()
    val order = state.order ?: return
    val checklist = state.checklist
    val layout = state.layout
    val scrollState = rememberScrollState()
    var arranging by rememberSaveable { mutableStateOf(false) }
    // Once there is an appointment to prepare for; a checklist already started stays reachable even if the
    // appointment later disappears from Tesla's data.
    val showChecklist = order.hasAppointment || checklist != null
    val blocks = BlockLayouts.visible(DetailBlock.entries, layout).filter {
        when (it) {
            DetailBlock.CHECKLIST -> showChecklist
            // Only while the ship carrying the car is known.
            DetailBlock.SHIPPING -> state.voyage != null
            else -> true
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Hero(order)
            Column(
                Modifier.padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                blocks.forEach { block ->
                    // Keyed so a moved section keeps its own composition (and its expand animation).
                    key(block) {
                        @Composable
                        fun section(content: @Composable () -> Unit) = Section(
                            stringResource(block.titleRes),
                            expanded = BlockLayouts.isExpanded(layout, block.id),
                            onToggle = { viewModel.handle(Action.OnToggleBlockExpanded(block)) },
                            content = content,
                        )
                        when (block) {
                            DetailBlock.CHECKLIST ->
                                ChecklistCard(deliveryDay = order.isDeliveryDay(), checklist = checklist, onOpen = onOpenChecklist)
                            DetailBlock.DELIVERY -> section { DeliveryContent(order, state.deliveryCenter) }
                            DetailBlock.SHIPPING -> state.voyage?.let { voyage -> section { ShippingContent(voyage, onOpenShip) } }
                            DetailBlock.PROGRESS_TRACKER -> section { StepTracker(order) }
                            DetailBlock.PROGRESS -> section { ProgressContent(order) }
                            DetailBlock.VEHICLE -> section { VehicleContent(order) }
                            DetailBlock.PAYMENT -> section { PaymentContent(order) }
                            DetailBlock.CONFIGURATION -> section { ConfigurationContent(order, state.options) }
                            DetailBlock.HISTORY -> section {
                                HistoryContent(
                                    history = state.history,
                                    filter = state.historyFilter,
                                    shown = state.shownHistory,
                                    onToggleImportantOnly = { viewModel.handle(Action.OnToggleImportantOnly) },
                                    onSelectCategory = { viewModel.handle(Action.OnSelectHistoryCategory(it)) },
                                )
                            }
                        }
                    }
                }
            }
        }
        // Drawn after the scrolling content so it stays pinned on top of it.
        StickyTopBar(
            title = stringResource(R.string.order_title, order.reference),
            scrollOffset = { scrollState.value },
            onBack = onBack,
            onArrange = { arranging = true },
        )
    }

    if (arranging) {
        DetailLayoutSheet(
            layout = layout,
            onMove = { block, delta -> viewModel.handle(Action.OnMoveBlock(block, delta)) },
            onToggle = { viewModel.handle(Action.OnToggleBlock(it)) },
            onReset = { viewModel.handle(Action.OnResetLayout) },
            onDismiss = { arranging = false },
        )
    }
}
