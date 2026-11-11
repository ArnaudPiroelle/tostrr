package app.tostrr.domain.checklist.usecase

import app.tostrr.domain.checklist.ChecklistReportWriter
import app.tostrr.domain.checklist.ChecklistRepository
import app.tostrr.domain.order.OrderRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import java.io.File

/** The PDF report of a locked checklist; null while it isn't locked, since the report must not change afterwards. */
@Factory
class CreateChecklistReportUseCase(
    private val checklists: ChecklistRepository,
    private val orders: OrderRepository,
    private val writer: ChecklistReportWriter,
) {
    suspend operator fun invoke(ref: String): File? {
        val checklist = checklists.observe(ref).first()?.takeIf { it.locked } ?: return null
        return writer.write(checklist, orders.orders.value.firstOrNull { it.reference == ref })
    }
}
