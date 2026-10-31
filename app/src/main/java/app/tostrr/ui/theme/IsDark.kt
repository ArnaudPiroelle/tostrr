package app.tostrr.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import app.tostrr.domain.appearance.ThemeMode

/** Whether this mode draws the app dark; [ThemeMode.SYSTEM] asks the phone. */
@Composable
fun ThemeMode.isDark(): Boolean = isDark(LocalConfiguration.current)

/** The same outside composition, before the first frame. */
fun ThemeMode.isDark(configuration: Configuration): Boolean = when (this) {
    ThemeMode.SYSTEM -> configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
