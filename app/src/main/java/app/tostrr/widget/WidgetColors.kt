package app.tostrr.widget

import androidx.glance.material3.ColorProviders
import app.tostrr.ui.theme.TostrrDarkColors
import app.tostrr.ui.theme.TostrrLightColors

/** The app's colour schemes for the widgets; without it Glance falls back to the system's dynamic colours. */
val TostrrWidgetColors = ColorProviders(light = TostrrLightColors, dark = TostrrDarkColors)
