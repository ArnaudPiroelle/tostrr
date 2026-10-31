package app.tostrr.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.tostrr.domain.appearance.usecase.ObserveThemeModeUseCase
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.widget.countdown.CountdownWidget
import app.tostrr.widget.order.OrderWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/** Redraws every home-screen widget each time the orders change (after every successful sync) or the theme does. */
@Single
class WidgetRefresher(
    private val context: Context,
    private val scope: CoroutineScope,
    private val observeOrdersUseCase: ObserveOrdersUseCase,
    private val observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val orderWidget: OrderWidget,
    private val countdownWidget: CountdownWidget,
) {

    /** Starts watching the orders and the theme for the life of the process; called once at startup. */
    fun start() {
        scope.launch {
            // The first values are those read at startup, which the widgets already show.
            merge(observeOrdersUseCase().drop(1), observeThemeModeUseCase().drop(1)).collect {
                orderWidget.updateAll(context)
                countdownWidget.updateAll(context)
            }
        }
    }
}
