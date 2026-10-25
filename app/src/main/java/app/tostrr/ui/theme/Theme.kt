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
    val Track: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /** Semantic colours for diffs: new value / old value. Green has no Material 3 role, so it adapts to the theme here. */
    val Positive: Color
        @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF7EDC9E) else Color(0xFF1B6E3C)
    val Negative: Color @Composable get() = MaterialTheme.colorScheme.error
}

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
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
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
