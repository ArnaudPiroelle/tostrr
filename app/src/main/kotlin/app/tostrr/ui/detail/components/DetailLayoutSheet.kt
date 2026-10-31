package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.ui.theme.TostrrColors

/** Reorders and hides the blocks of the order detail. Every change applies at once; "Réinitialiser" undoes them all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailLayoutSheet(
    layout: BlockLayout,
    onMove: (DetailBlock, Int) -> Unit,
    onToggle: (DetailBlock) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shown = BlockLayouts.visible(DetailBlock.entries, layout)
    val hidden = BlockLayouts.hidden(DetailBlock.entries, layout)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onReset, enabled = !layout.isArrangementDefault) { Text(stringResource(R.string.detail_arrange_reset)) }
            Text(
                stringResource(R.string.detail_arrange),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok), fontWeight = FontWeight.SemiBold) }
        }
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { GroupTitle(stringResource(R.string.detail_arrange_shown)) }
            if (shown.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.detail_arrange_all_hidden),
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TostrrColors.Muted,
                    )
                }
            }
            shown.forEachIndexed { index, block ->
                item(key = "shown-${block.id}") {
                    BlockRow(
                        block,
                        hidden = false,
                        canMoveUp = index > 0,
                        canMoveDown = index < shown.lastIndex,
                        onMove = { onMove(block, it) },
                        onToggle = { onToggle(block) },
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.detail_arrange_hint),
                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TostrrColors.Muted,
                )
            }
            if (hidden.isNotEmpty()) {
                item { GroupTitle(stringResource(R.string.detail_arrange_hidden)) }
                hidden.forEach { block ->
                    item(key = "hidden-${block.id}") {
                        BlockRow(block, hidden = true, canMoveUp = false, canMoveDown = false, onMove = {}, onToggle = { onToggle(block) })
                    }
                }
            }
        }
    }
}
