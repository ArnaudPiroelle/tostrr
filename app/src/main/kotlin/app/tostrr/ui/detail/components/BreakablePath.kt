package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.key

/** Zero-width spaces after the dots let a long key path wrap at a segment boundary. */
internal fun breakablePath(path: String) = path.replace(".", ".\u200B")
