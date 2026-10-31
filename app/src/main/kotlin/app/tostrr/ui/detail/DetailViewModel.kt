package app.tostrr.ui.detail

import androidx.lifecycle.viewModelScope
import app.tostrr.domain.checklist.Checklist
import app.tostrr.domain.checklist.usecase.ObserveChecklistUseCase
import app.tostrr.domain.deliverycenter.DeliveryCenter
import app.tostrr.domain.deliverycenter.usecase.FindDeliveryCenterUseCase
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.domain.layout.usecase.MoveDetailBlockUseCase
import app.tostrr.domain.layout.usecase.ObserveDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ResetDetailLayoutUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockExpandedUseCase
import app.tostrr.domain.layout.usecase.ToggleDetailBlockUseCase
import app.tostrr.domain.order.Change
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOptionCodesUseCase
import app.tostrr.domain.order.usecase.ObserveOrderHistoryUseCase
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.ui.architecture.BaseAction
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoSideEffect
import app.tostrr.ui.detail.DetailViewModel.Action
import app.tostrr.ui.detail.DetailViewModel.State
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

/** One order's detail, and how the user arranged its blocks (shared by every order). */
@KoinViewModel
class DetailViewModel(
    /** Given by the screen, from its navigation key. */
    @InjectedParam private val reference: String,
    observeOrdersUseCase: ObserveOrdersUseCase,
    observeOrderHistoryUseCase: ObserveOrderHistoryUseCase,
    observeOptionCodesUseCase: ObserveOptionCodesUseCase,
    observeChecklistUseCase: ObserveChecklistUseCase,
    observeDetailLayoutUseCase: ObserveDetailLayoutUseCase,
    private val findDeliveryCenterUseCase: FindDeliveryCenterUseCase,
    private val moveDetailBlockUseCase: MoveDetailBlockUseCase,
    private val toggleDetailBlockUseCase: ToggleDetailBlockUseCase,
    private val resetDetailLayoutUseCase: ResetDetailLayoutUseCase,
    private val toggleDetailBlockExpandedUseCase: ToggleDetailBlockExpandedUseCase,
) : BaseViewModel<Action, State, NoSideEffect>() {

    private val order = observeOrdersUseCase().map { orders -> orders.find(reference) }

    private val deliveryCenter = order.filterNotNull()
        .map { it.deliveryCenter to it.countryCode }
        .distinctUntilChanged()
        .map { (center, country) -> findDeliveryCenterUseCase(center, country) }

    private val content = combine(
        order,
        observeOrderHistoryUseCase().map { it[reference].orEmpty() },
        observeOptionCodesUseCase(),
        observeChecklistUseCase(reference),
        observeDetailLayoutUseCase(),
    ) { order, history, options, checklist, layout -> State(order, history, options, checklist, layout = layout) }

    // Starts from the current order and layout, so the screen opens already filled.
    override val state: StateFlow<State> = combine(content, deliveryCenter.onStart { emit(null) }) { content, center ->
        content.copy(deliveryCenter = center)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State(
            order = observeOrdersUseCase().value.find(reference),
            history = observeOrderHistoryUseCase().value[reference].orEmpty(),
            options = observeOptionCodesUseCase().value,
            layout = observeDetailLayoutUseCase().value,
        ),
    )

    private fun List<Order>.find(reference: String) = firstOrNull { it.reference == reference }

    override suspend fun onHandle(action: Action) {
        when (action) {
            is Action.OnMoveBlock -> moveDetailBlockUseCase(action.block, action.delta)
            is Action.OnToggleBlock -> toggleDetailBlockUseCase(action.block)
            is Action.OnToggleBlockExpanded -> toggleDetailBlockExpandedUseCase(action.block)
            Action.OnResetLayout -> resetDetailLayoutUseCase()
        }
    }

    data class State(
        /** Null while unknown, or once the order is gone (after a logout). */
        val order: Order? = null,
        val history: List<Change> = emptyList(),
        val options: Map<String, OptionInfo> = emptyMap(),
        val checklist: Checklist? = null,
        val deliveryCenter: DeliveryCenter? = null,
        val layout: BlockLayout = BlockLayout(),
    ) : BaseState

    sealed class Action : BaseAction {
        /** Moves a shown block up ([delta] < 0) or down among the shown ones. */
        data class OnMoveBlock(val block: DetailBlock, val delta: Int) : Action()
        data class OnToggleBlock(val block: DetailBlock) : Action()
        data class OnToggleBlockExpanded(val block: DetailBlock) : Action()
        data object OnResetLayout : Action()
    }
}
