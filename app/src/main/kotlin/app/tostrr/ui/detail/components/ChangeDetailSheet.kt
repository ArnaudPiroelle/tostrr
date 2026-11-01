package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tostrr.R
import app.tostrr.domain.order.Change
import app.tostrr.ui.theme.TostrrColors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val PrettyJson = Json { prettyPrint = true }

private fun prettyJson(text: String): String =
    runCatching { PrettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(text)) }.getOrDefault(text)

/** Bottom sheet showing the JSON that changed, before and after. Opens half-height, drags up to full, swipes down to close. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangeDetailSheet(change: Change, onDismiss: () -> Unit) {
    val hasOld = change.oldValue != null
    val hasNew = change.newValue != null
    var showAfter by rememberSaveable { mutableStateOf(!hasOld) }
    val lines = remember(change, showAfter) {
        prettyJson((if (showAfter) change.newValue else change.oldValue).orEmpty()).lines()
    }
    val color = if (showAfter) TostrrColors.Positive else TostrrColors.Negative

    // Opens half-height; drag up to expand to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    // Full screen: no drag handle (the × closes it), and the content starts below the status bar.
    val expanded = sheetState.targetValue == SheetValue.Expanded

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = if (expanded) null else ({ BottomSheetDefaults.DragHandle() }),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.statusBarsPadding() else Modifier)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    breakablePath(change.key),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close)) }
            }
            if (hasOld && hasNew) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showAfter, onClick = { showAfter = false }, label = { Text(stringResource(R.string.detail_change_before)) })
                    FilterChip(selected = showAfter, onClick = { showAfter = true }, label = { Text(stringResource(R.string.detail_change_after)) })
                }
            }
            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(lines) { line ->
                    Text(
                        line,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = color,
                    )
                }
            }
        }
    }
}
