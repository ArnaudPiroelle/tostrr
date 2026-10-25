package com.tostrr.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.tostrr.widget.countdown.CountdownWidget
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Instantiated by Android, so it can't take constructor parameters: it fetches its widget from Koin.
 * Keep it in this package: launchers identify placed widgets by the receiver's class name, so moving it would break them.
 */
class CountdownWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {
    override val glanceAppWidget: GlanceAppWidget by inject<CountdownWidget>()
}
