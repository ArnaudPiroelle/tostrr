package app.tostrr.widget.countdown

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.tostrr.domain.order.deliveryCountdown
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.widget.OrderRefKey
import app.tostrr.widget.countdown.components.CountdownWidgetContent
import org.koin.core.annotation.Single

/** 2x1 countdown to the delivery: days to the appointment, or to / inside the delivery window while there is none. */
@Single
class CountdownWidget(private val observeOrdersUseCase: ObserveOrdersUseCase) : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
                provideContent {
            val chosen = currentState<Preferences>()[OrderRefKey]
            val orders by observeOrdersUseCase().collectAsState()
            val order = orders.firstOrNull { it.reference == chosen } ?: orders.firstOrNull()
            // Today is read on every composition: each refresh's updateAll moves the count past midnight.
            GlanceTheme { CountdownWidgetContent(order, order?.deliveryCountdown()?.let(::countdownLabels)) }
        }
    }
}
