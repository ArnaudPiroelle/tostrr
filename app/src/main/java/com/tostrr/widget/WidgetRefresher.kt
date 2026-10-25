package com.tostrr.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.tostrr.data.OrderRefreshListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/** Redraws every home-screen widget once a sync has updated the orders. */
@Single
class WidgetRefresher(
    private val context: Context,
    private val scope: CoroutineScope,
) : OrderRefreshListener {

    override fun onOrdersRefreshed() {
        scope.launch {
            OrderWidget().updateAll(context)
            CountdownWidget().updateAll(context)
        }
    }
}
