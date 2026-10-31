package app.tostrr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Short names for the Material 3 roles used across screens; they follow the active light/dark scheme. */
object TostrrColors {
    val Background: Color @Composable get() = MaterialTheme.colorScheme.background
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val SurfaceHigh: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val Border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
    val OnSurface: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    /** Unreached steps and empty progress, as on the widgets: a container colour vanishes on the light cards. */
    val Track: Color @Composable get() = MaterialTheme.colorScheme.outline

    /** Semantic colours for diffs: new value / old value. Green has no Material 3 role, so it adapts to the theme here. */
    val Positive: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF7EDC9E) else Color(0xFF1B6E3C)
    val Negative: Color @Composable get() = MaterialTheme.colorScheme.error
}

/** Tesla's red. The light scheme uses a deeper shade so white text on it stays readable. */
private val TeslaRed = Color(0xFFE31937)
private val TeslaRedDeep = Color(0xFFC8102E)

/** Near-black and neutral greys around Tesla red, without Material's default violet tint. */
internal val TostrrDarkColors = darkColorScheme(
    primary = TeslaRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0A16),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFC4C4C9),
    onSecondary = Color(0xFF1B1B1E),
    secondaryContainer = Color(0xFF2E2E33),
    onSecondaryContainer = Color(0xFFE4E4E8),
    tertiary = Color(0xFFC4C4C9),
    background = Color(0xFF0A0A0B),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF0A0A0B),
    onSurface = Color(0xFFEDEDEF),
    surfaceVariant = Color(0xFF2A2A2E),
    onSurfaceVariant = Color(0xFFA1A1A8),
    surfaceContainerLowest = Color(0xFF050506),
    surfaceContainerLow = Color(0xFF111113),
    surfaceContainer = Color(0xFF161618),
    surfaceContainerHigh = Color(0xFF1E1E21),
    surfaceContainerHighest = Color(0xFF2A2A2E),
    outline = Color(0xFF5C5C63),
    outlineVariant = Color(0xFF2E2E33),
)

internal val TostrrLightColors = lightColorScheme(
    primary = TeslaRedDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADA),
    onPrimaryContainer = Color(0xFF410007),
    secondary = Color(0xFF5E5E63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6EA),
    onSecondaryContainer = Color(0xFF1B1B1E),
    tertiary = Color(0xFF5E5E63),
    background = Color.White,
    onBackground = Color(0xFF171719),
    surface = Color.White,
    onSurface = Color(0xFF171719),
    surfaceVariant = Color(0xFFE6E6EA),
    onSurfaceVariant = Color(0xFF5E5E63),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F9),
    surfaceContainer = Color(0xFFF3F3F5),
    surfaceContainerHigh = Color(0xFFECECEF),
    surfaceContainerHighest = Color(0xFFE4E4E8),
    outline = Color(0xFF8E8E94),
    outlineVariant = Color(0xFFD6D6DB),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
)

@Composable
fun TostrrTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) TostrrDarkColors else TostrrLightColors,
        typography = AppTypography,
    ) {
        // Gives every screen a default content colour (otherwise bare Text/Icon fall back to black).
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
