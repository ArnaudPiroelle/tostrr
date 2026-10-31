package app.tostrr.ui.settings.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors
import coil3.compose.AsyncImage
import java.io.File

/** The account picture from its local copy, round; the generic icon while it loads, or when there is none. */
@Composable
internal fun Avatar(path: String?) {
    val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
    // Only the icon is tinted, never the photo.
    var loaded by remember(path) { mutableStateOf(false) }
    AsyncImage(
        model = path?.let(::File),
        contentDescription = null,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        onLoading = { loaded = false },
        onSuccess = { loaded = true },
        onError = { loaded = false },
        contentScale = ContentScale.Crop,
        colorFilter = if (loaded) null else ColorFilter.tint(TostrrColors.Muted),
        modifier = Modifier.size(40.dp).clip(CircleShape),
    )
}
