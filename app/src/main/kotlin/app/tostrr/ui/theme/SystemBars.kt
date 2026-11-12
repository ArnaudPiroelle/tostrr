package app.tostrr.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Draws behind transparent system bars, with icons readable on the app's theme rather than the phone's; on what is
 * under the status bar when [darkStatusBar] says it is dark whatever the theme (the ship screen's globe).
 */
fun ComponentActivity.applySystemBars(dark: Boolean, darkStatusBar: Boolean = dark) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkStatusBar },
        // The scrims `enableEdgeToEdge` uses by default, shown behind three-button navigation.
        navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
    )
}

private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
