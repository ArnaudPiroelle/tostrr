package app.tostrr.ui.ship.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R

/**
 * A round back arrow floating over the globe: white in the light theme, dark in the dark one, a shade lighter than the
 * dark surfaces so it stands out on the night sky.
 */
@Composable
internal fun ShipBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background = if (colors.background.luminance() < 0.5f) colors.surfaceContainerHighest else colors.surface
    Surface(modifier, shape = CircleShape, color = background, shadowElevation = 4.dp) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
    }
}
