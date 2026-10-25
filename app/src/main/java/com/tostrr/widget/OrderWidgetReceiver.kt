package com.tostrr.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Instantiated by Android, so it can't take constructor parameters: it fetches its widget from Koin. */
class OrderWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {
    override val glanceAppWidget: GlanceAppWidget by inject<OrderWidget>()
}
