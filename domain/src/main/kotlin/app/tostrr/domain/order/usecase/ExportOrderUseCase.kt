package app.tostrr.domain.order.usecase

import app.tostrr.domain.order.OrderExport
import app.tostrr.domain.order.OrderExportWriter
import app.tostrr.domain.order.OrderRepository
import org.koin.core.annotation.Factory
import java.io.File

/** The JSON export of order [ref], with its history; null when the order is no longer known. */
@Factory
class ExportOrderUseCase(
    private val orders: OrderRepository,
    private val writer: OrderExportWriter,
) {
    suspend operator fun invoke(ref: String, now: Long = System.currentTimeMillis()): File? {
        val order = orders.orders.value.firstOrNull { it.reference == ref } ?: return null
        return writer.write(ref, OrderExport.json(order, orders.history.value[ref].orEmpty(), now))
    }
}
