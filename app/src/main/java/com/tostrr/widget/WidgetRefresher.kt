package com.tostrr.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.tostrr.domain.order.usecase.ObserveOrdersUseCase
import com.tostrr.widget.countdown.CountdownWidget
import com.tostrr.widget.order.OrderWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/** Redraws every home-screen widget each time the orders change, i.e. after every successful sync. */
@Single
class WidgetRefresher(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observeOrdersUseCase: ObserveOrdersUseCase,
    private val orderWidget: OrderWidget,
    private val countdownWidget: CountdownWidget,
) {

    /** Starts watching the orders for the life of the process; called once at startup. */
    fun start() {
        scope.launch {
            // The first value is the cache read at startup, which the widgets already show.
            observeOrdersUseCase().drop(1).collect {
                orderWidget.updateAll(context)
                countdownWidget.updateAll(context)
            }
        }
    }
}
