package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.layout.DetailBlock
import app.tostrr.ui.theme.TostrrColors

/** Name in the arrangement sheet, where the two progress views need telling apart. */
@get:StringRes
private val DetailBlock.labelRes: Int
    get() = when (this) {
        DetailBlock.PROGRESS_TRACKER -> R.string.detail_block_progress_tracker_label
        DetailBlock.PROGRESS -> R.string.detail_block_progress_label
        else -> titleRes
    }

@get:StringRes
private val DetailBlock.blurbRes: Int
    get() = when (this) {
        DetailBlock.CHECKLIST -> R.string.detail_block_checklist_blurb
        DetailBlock.DELIVERY -> R.string.detail_block_delivery_blurb
        DetailBlock.PROGRESS_TRACKER -> R.string.detail_block_progress_tracker_blurb
        DetailBlock.PROGRESS -> R.string.detail_block_progress_blurb
        DetailBlock.VEHICLE -> R.string.detail_block_vehicle_blurb
        DetailBlock.PAYMENT -> R.string.detail_block_payment_blurb
        DetailBlock.CONFIGURATION -> R.string.detail_block_configuration_blurb
        DetailBlock.HISTORY -> R.string.detail_block_history_blurb
    }

private val DetailBlock.icon: ImageVector
    get() = when (this) {
        DetailBlock.CHECKLIST -> Icons.Outlined.FactCheck
        DetailBlock.DELIVERY -> Icons.Outlined.LocalShipping
        DetailBlock.PROGRESS_TRACKER -> Icons.Outlined.LinearScale
        DetailBlock.PROGRESS -> Icons.Outlined.Timeline
        DetailBlock.VEHICLE -> Icons.Outlined.DirectionsCar
        DetailBlock.PAYMENT -> Icons.Outlined.Payments
        DetailBlock.CONFIGURATION -> Icons.Outlined.Tune
        DetailBlock.HISTORY -> Icons.Outlined.History
    }

@Composable
internal fun BlockRow(
    block: DetailBlock,
    hidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onToggle: () -> Unit,
) {
    val label = stringResource(block.labelRes)
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
                label,
                style = MaterialTheme.typography.titleMedium,
                color = if (hidden) TostrrColors.Muted else TostrrColors.OnSurface,
            )
            Text(stringResource(block.blurbRes), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted, maxLines = 2)
        }
        if (!hidden) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.detail_arrange_move_up, label))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.detail_arrange_move_down, label))
            }
        }
        IconButton(onClick = onToggle) {
            if (hidden) {
                Icon(Icons.Outlined.AddCircleOutline, contentDescription = stringResource(R.string.detail_arrange_show, label), tint = TostrrColors.Accent)
            } else {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = stringResource(R.string.detail_arrange_hide, label), tint = TostrrColors.Muted)
            }
        }
    }
}
