package app.tostrr.ui.common

import androidx.annotation.StringRes
import app.tostrr.R
import app.tostrr.domain.order.StepKind

/** The name of a step, under its dot in the trackers and in the vertical progress. */
@get:StringRes
internal val StepKind.labelRes: Int
    get() = when (this) {
        StepKind.ORDERED -> R.string.step_ordered
        StepKind.VIN_ASSIGNED -> R.string.step_vin_assigned
        StepKind.BUILT -> R.string.step_built
        StepKind.DOCUMENTS_SENT -> R.string.step_documents_sent
        StepKind.REGISTERED -> R.string.step_registered
        StepKind.DELIVERY -> R.string.step_delivery
    }
