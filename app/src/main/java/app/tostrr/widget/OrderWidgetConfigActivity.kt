package app.tostrr.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import app.tostrr.R
import app.tostrr.domain.appearance.usecase.ObserveThemeModeUseCase
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.usecase.ObserveOrdersUseCase
import app.tostrr.ui.theme.TostrrTheme
import app.tostrr.ui.theme.applySystemBars
import app.tostrr.ui.theme.isDark
import app.tostrr.widget.countdown.CountdownWidget
import app.tostrr.widget.order.OrderWidget
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Shown when an order or countdown widget is added (and when it is reconfigured): lets the user pick which order it displays. */
class OrderWidgetConfigActivity : ComponentActivity() {

    private val observeOrdersUseCase: ObserveOrdersUseCase by inject()
    private val observeThemeModeUseCase: ObserveThemeModeUseCase by inject()
    private val orderWidget: OrderWidget by inject()
    private val countdownWidget: CountdownWidget by inject()
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A short-lived picker: the theme chosen when it opens is kept.
        val dark = observeThemeModeUseCase().value.isDark(resources.configuration)
        applySystemBars(dark)
        // If the user backs out, the launcher must not place the widget.
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val orders = observeOrdersUseCase().value
        if (orders.size == 1) {
            choose(orders.first())
            return
        }

        setContent {
            TostrrTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 16.dp)) {
                    Text(
                        stringResource(R.string.widget_config_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (orders.isEmpty()) {
                        Text(
                            stringResource(R.string.widget_config_empty),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyColumn {
                        items(orders, key = { it.reference }) { order ->
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.widget_config_order, order.modelName, order.reference)) },
                                supportingContent = { Text(order.deliverySlot?.let { stringResource(R.string.order_slot, it) } ?: stringResource(R.string.widget_config_vin, order.vin ?: stringResource(R.string.order_vin_unknown))) },
                                modifier = Modifier.clickable { choose(order) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    private fun choose(order: Order) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@OrderWidgetConfigActivity).getGlanceIdBy(widgetId)
            updateAppWidgetState(this@OrderWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) { prefs: Preferences ->
                prefs.toMutablePreferences().apply { this[OrderRefKey] = order.reference }
            }
            // Both widgets share this screen; only the one being configured knows how to draw itself.
            val provider = AppWidgetManager.getInstance(this@OrderWidgetConfigActivity).getAppWidgetInfo(widgetId)?.provider
            val widget = if (provider?.className == CountdownWidgetReceiver::class.java.name) countdownWidget else orderWidget
            widget.update(this@OrderWidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
