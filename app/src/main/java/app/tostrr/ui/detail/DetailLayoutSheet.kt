package app.tostrr.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.domain.layout.BlockLayout
import app.tostrr.domain.layout.BlockLayouts
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.ui.theme.TostrrColors

/** Title of a block's section in the detail screen; both progress views share theirs. */
internal val DetailBlock.title: String
    get() = when (this) {
        DetailBlock.CHECKLIST -> "Checklist de livraison"
        DetailBlock.DELIVERY -> "Informations de livraison"
        DetailBlock.PROGRESS_TRACKER, DetailBlock.PROGRESS -> "Progression de la commande"
        DetailBlock.CONFIGURATION -> "Configuration"
        DetailBlock.HISTORY -> "Historique des modifications"
    }

/** Name in the arrangement sheet, where the two progress views need telling apart. */
private val DetailBlock.label: String
    get() = when (this) {
        DetailBlock.PROGRESS_TRACKER -> "$title (horizontale)"
        DetailBlock.PROGRESS -> "$title (verticale)"
        else -> title
    }

private val DetailBlock.blurb: String
    get() = when (this) {
        DetailBlock.CHECKLIST -> "Affichée dès qu'un rendez-vous de livraison est fixé."
        DetailBlock.DELIVERY -> "Centre et fenêtre de livraison."
        DetailBlock.PROGRESS_TRACKER -> "Les étapes sur une ligne, comme dans la liste."
        DetailBlock.PROGRESS -> "Les étapes l'une sous l'autre, avec leurs dates."
        DetailBlock.CONFIGURATION -> "Les options de la voiture."
        DetailBlock.HISTORY -> "Ce qui a changé d'une synchronisation à l'autre."
    }

private val DetailBlock.icon: ImageVector
    get() = when (this) {
        DetailBlock.CHECKLIST -> Icons.Outlined.FactCheck
        DetailBlock.DELIVERY -> Icons.Outlined.LocalShipping
        DetailBlock.PROGRESS_TRACKER -> Icons.Outlined.LinearScale
        DetailBlock.PROGRESS -> Icons.Outlined.Timeline
        DetailBlock.CONFIGURATION -> Icons.Outlined.Tune
        DetailBlock.HISTORY -> Icons.Outlined.History
    }

/** Reorders and hides the blocks of the order detail. Every change applies at once; "Réinitialiser" undoes them all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLayoutSheet(
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
            TextButton(onClick = onReset, enabled = !layout.isArrangementDefault) { Text("Réinitialiser") }
            Text(
                "Organiser la page",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onDismiss) { Text("OK", fontWeight = FontWeight.SemiBold) }
        }
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { GroupTitle("Affichées") }
            if (shown.isEmpty()) {
                item {
                    Text(
                        "Tous les blocs sont masqués. Réaffichez-en un ci-dessous.",
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
                    "Utilisez les flèches pour changer l'ordre. Un bloc n'apparaît que s'il a quelque chose à montrer.",
                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TostrrColors.Muted,
                )
            }
            if (hidden.isNotEmpty()) {
                item { GroupTitle("Masquées") }
                hidden.forEach { block ->
                    item(key = "hidden-${block.id}") {
                        BlockRow(block, hidden = true, canMoveUp = false, canMoveDown = false, onMove = {}, onToggle = { onToggle(block) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text.uppercase(),
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = TostrrColors.Muted,
    )
}

@Composable
private fun BlockRow(
    block: DetailBlock,
    hidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            block.icon,
            contentDescription = null,
            tint = if (hidden) TostrrColors.Muted else TostrrColors.Accent,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                block.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (hidden) TostrrColors.Muted else TostrrColors.OnSurface,
            )
            Text(block.blurb, style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted, maxLines = 2)
        }
        if (!hidden) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Monter « ${block.label} »")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Descendre « ${block.label} »")
            }
        }
        IconButton(onClick = onToggle) {
            if (hidden) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Afficher « ${block.label} »", tint = TostrrColors.Accent)
            } else {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = "Masquer « ${block.label} »", tint = TostrrColors.Muted)
            }
        }
    }
}
