package app.tostrr.ui.detail.components

import androidx.annotation.StringRes
import app.tostrr.R
import app.tostrr.domain.layout.DetailBlock

/** Title of a block's section in the detail screen; both progress views share theirs. */
@get:StringRes
internal val DetailBlock.titleRes: Int
    get() = when (this) {
        DetailBlock.CHECKLIST -> R.string.detail_block_checklist
        DetailBlock.DELIVERY -> R.string.detail_block_delivery
        DetailBlock.PROGRESS_TRACKER, DetailBlock.PROGRESS -> R.string.detail_block_progress
        DetailBlock.VEHICLE -> R.string.detail_block_vehicle
        DetailBlock.PAYMENT -> R.string.detail_block_payment
        DetailBlock.CONFIGURATION -> R.string.detail_block_configuration
        DetailBlock.HISTORY -> R.string.detail_block_history
    }
