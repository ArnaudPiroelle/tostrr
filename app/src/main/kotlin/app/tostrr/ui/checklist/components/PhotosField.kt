package app.tostrr.ui.checklist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import coil3.compose.AsyncImage
import java.io.File

private const val PHOTOS_PER_ROW = 3

/** The photos taken so far, three per row with a delete button each, then the dashed zone to add more. */
@Composable
internal fun PhotosField(photos: List<String>, onTakePhoto: () -> Unit, onPickImage: () -> Unit, onDelete: (path: String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.chunked(PHOTOS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { photo ->
                    Box(Modifier.weight(1f)) {
                        AsyncImage(
                            model = File(photo),
                            contentDescription = stringResource(R.string.common_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp),
                        ) {
                            IconButton(onClick = { onDelete(photo) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.checklist_photo_delete), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                // Keeps the last row's photos as wide as the others.
                repeat(PHOTOS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        AddPhotoZone(compact = photos.isNotEmpty(), onTakePhoto = onTakePhoto, onPickImage = onPickImage)
    }
}
