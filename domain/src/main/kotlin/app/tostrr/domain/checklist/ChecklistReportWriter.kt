package app.tostrr.domain.checklist

import app.tostrr.domain.order.Order
import java.io.File

/**
 * Writes the report of a locked checklist as a PDF, worded in the app's language. Implemented in `:app`, where the
 * checklist's texts are.
 */
interface ChecklistReportWriter {
    /** [order] is null when the order is no longer known; the report then names it by its reference only. */
    suspend fun write(checklist: Checklist, order: Order?): File
}
