package app.tostrr.ui.ship

import androidx.lifecycle.viewModelScope
import app.tostrr.di.AppModule
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.domain.shipping.ShipSnapshot
import app.tostrr.domain.shipping.ShipVoyages
import app.tostrr.domain.shipping.TimelineEntry
import app.tostrr.domain.shipping.Voyage
import app.tostrr.domain.shipping.VoyageTimeline
import app.tostrr.domain.shipping.usecase.ObserveShipSnapshotUseCase
import app.tostrr.ui.architecture.BaseState
import app.tostrr.ui.architecture.BaseViewModel
import app.tostrr.ui.architecture.NoAction
import app.tostrr.ui.architecture.NoSideEffect
import app.tostrr.ui.common.GlobeLane
import app.tostrr.ui.navigation.Screen
import app.tostrr.ui.ship.ShipViewModel.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Named

/** One of the ships that may carry an order's car, while it still may. */
@KoinViewModel
class ShipViewModel(
    /** Given by the screen: its navigation key. */
    @InjectedParam private val key: Screen.Ship,
    observeOrdersUseCase: ObserveOrdersUseCase,
    observeShipSnapshotUseCase: ObserveShipSnapshotUseCase,
    @Named(AppModule.BACKGROUND) background: CoroutineDispatcher,
) : BaseViewModel<NoAction, State, NoSideEffect>() {

    // Starts from the current orders and snapshot, so the screen opens already filled; the globe's lane follows,
    // built off the main thread.
    override val state: StateFlow<State> = combine(observeOrdersUseCase(), observeShipSnapshotUseCase()) { orders, snapshot ->
        stateOf(orders, snapshot, withLane = true)
    }
        .flowOn(background)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            stateOf(observeOrdersUseCase().value, observeShipSnapshotUseCase().value, withLane = false),
        )

    private fun stateOf(orders: List<Order>, snapshot: ShipSnapshot?, withLane: Boolean): State {
        val voyage = orders.firstOrNull { it.reference == key.reference }?.let { ShipVoyages.of(it, snapshot) } ?: return State()
        val index = voyage.candidates.indexOfFirst { it.ship.name == key.ship }
        if (index < 0) return State()
        val candidate = voyage.candidates[index]
        return State(
            voyage = candidate,
            lane = if (withLane) GlobeLane.of(candidate) else null,
            timeline = VoyageTimeline.of(candidate.ship),
            candidateIndex = index,
            candidates = voyage.candidates.size,
            vinBlock = voyage.vinBlock,
            snapshotAtEpochMs = voyage.snapshotAtEpochMs,
        )
    }

    override suspend fun onHandle(action: NoAction) = Unit

    data class State(
        /** Null once the ship no longer may carry the car: unloaded, given up by the site, or the car delivered. */
        val voyage: Voyage? = null,
        /** The [voyage]'s lane on the globe, once built. */
        val lane: GlobeLane? = null,
        val timeline: List<TimelineEntry> = emptyList(),
        /** Its place among the ships that may carry the car, which gives its colour as in the order detail. */
        val candidateIndex: Int = 0,
        val candidates: Int = 0,
        val vinBlock: String? = null,
        val snapshotAtEpochMs: Long = 0L,
    ) : BaseState
}
