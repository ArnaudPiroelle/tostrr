package app.tostrr.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * The launcher artwork without its background: a red dot over a T. Drawn here rather than as a drawable so its
 * colours follow the theme picked in the app, not the phone's night mode that resources follow.
 */
@Composable
internal fun TostrrLogo(modifier: Modifier = Modifier.size(28.dp, 32.dp)) {
    val dot = MaterialTheme.colorScheme.primary
    val text = MaterialTheme.colorScheme.onBackground
    Canvas(modifier) {
        // Laid out on a 60 x 68 grid, scaled to the given size.
        val sx = size.width / 60f
        val sy = size.height / 68f
        drawCircle(dot, radius = 10f * minOf(sx, sy), center = Offset(30f * sx, 10f * sy))
        drawRect(text, topLeft = Offset(0f, 32f * sy), size = Size(60f * sx, 8f * sy))
        drawRect(text, topLeft = Offset(26f * sx, 40f * sy), size = Size(8f * sx, 28f * sy))
    }
}
