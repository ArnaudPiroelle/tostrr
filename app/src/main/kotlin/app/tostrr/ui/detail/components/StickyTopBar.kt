package app.tostrr.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R

/** Scroll distance (px) over which the top bar goes from transparent to fully opaque. */
private const val TOP_BAR_FADE_PX = 240f

/** Pinned top bar: transparent over the hero, then blends into the screen colour as content scrolls under it. */
@Composable
internal fun StickyTopBar(title: String, scrollOffset: () -> Int, onBack: () -> Unit, onArrange: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    // Alpha is read while drawing, so scrolling doesn't recompose the bar.
    fun progress() = (scrollOffset() / TOP_BAR_FADE_PX).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(background.copy(alpha = progress())) }
                .statusBarsPadding()
                .height(TOP_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                title,
                Modifier.weight(1f).graphicsLayer { alpha = progress() },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            IconButton(onClick = onArrange) {
                Icon(Icons.Outlined.DashboardCustomize, contentDescription = stringResource(R.string.detail_arrange))
            }
        }
        // Separates the opaque bar from the screen background; fades in with the bar so the hero stays clean.
        HorizontalDivider(
            Modifier.graphicsLayer { alpha = progress() },
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
