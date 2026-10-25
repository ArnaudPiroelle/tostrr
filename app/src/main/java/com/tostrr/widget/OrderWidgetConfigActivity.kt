package com.tostrr.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.tostrr.domain.order.Order
import com.tostrr.domain.order.usecase.ObserveOrdersUseCase
import com.tostrr.ui.theme.TostrrTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Shown when an order or countdown widget is added (and when it is reconfigured): lets the user pick which order it displays. */
class OrderWidgetConfigActivity : ComponentActivity() {

    private val observeOrdersUseCase: ObserveOrdersUseCase by inject()
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            TostrrTheme {
                Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 16.dp)) {
                    Text(
                        "Quelle commande afficher ?",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (orders.isEmpty()) {
                        Text(
                            "Aucune commande pour le moment. Ouvrez Tostrr et connectez-vous.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyColumn {
                        items(orders, key = { it.reference }) { order ->
                            ListItem(
                                headlineContent = { Text("${order.modelName} · #${order.reference}") },
                                supportingContent = { Text(order.deliverySlot?.let { "Créneau : $it" } ?: "VIN : ${order.vin ?: "inconnu"}") },
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
            val widget = if (provider?.className == CountdownWidgetReceiver::class.java.name) CountdownWidget() else OrderWidget()
            widget.update(this@OrderWidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
