package app.tostrr.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/** Draws behind transparent system bars, with icons readable on the app's theme rather than the phone's. */
fun ComponentActivity.applySystemBars(dark: Boolean) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
        // The scrims `enableEdgeToEdge` uses by default, shown behind three-button navigation.
        navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
    )
}

private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
