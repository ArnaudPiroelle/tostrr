package app.tostrr.ui.settings.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.appearance.ThemeMode

@Composable
internal fun AppearanceSection(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    SettingsSection(stringResource(R.string.settings_appearance_section)) {
        val labels = ThemeMode.entries.associateWith { stringResource(it.labelRes) }
        SegmentedRow(
            title = stringResource(R.string.settings_theme),
            summary = stringResource(R.string.settings_theme_summary),
            selected = themeMode,
            options = ThemeMode.entries,
            label = labels::getValue,
            icon = { it.icon },
            onSelect = onThemeChange,
        )
    }
}

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Outlined.LightMode
        ThemeMode.DARK -> Icons.Outlined.DarkMode
    }
