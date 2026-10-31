package app.tostrr.widget

import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import app.tostrr.domain.appearance.ThemeMode
import app.tostrr.ui.theme.TostrrDarkColors
import app.tostrr.ui.theme.TostrrLightColors

/**
 * The app's colour schemes for the widgets, following the theme picked in the settings; without them Glance falls back
 * to the system's dynamic colours. [ThemeMode.SYSTEM] leaves the launcher to switch with the phone.
 */
fun widgetColors(mode: ThemeMode): ColorProviders = when (mode) {
    ThemeMode.SYSTEM -> ColorProviders(light = TostrrLightColors, dark = TostrrDarkColors)
    ThemeMode.LIGHT -> ColorProviders(TostrrLightColors)
    ThemeMode.DARK -> ColorProviders(TostrrDarkColors)
}
