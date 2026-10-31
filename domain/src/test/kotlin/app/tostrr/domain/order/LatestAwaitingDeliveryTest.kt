package app.tostrr.domain.order

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LatestAwaitingDeliveryTest {

    private fun order(reference: String, status: String = "BOOKED", bookedOn: String? = null) = Order(
        reference = reference, status = status, vin = null, modelName = "Model Y", optionCodes = emptyList(), imageUrl = null,
        steps = emptyList(), deliveryCenter = null, deliveryWindow = null, deliverySlot = null,
        appointment = null, etaToDeliveryCenter = null, expectedRegistration = null, fetchedAtEpochMs = 0L,
        raw = JsonObject(emptyMap()), bookedOn = bookedOn?.let(LocalDate::parse),
    )

    private fun List<Order>.refs() = map { it.reference }

    @Test fun `keeps the newest orders first, up to the limit`() {
        val orders = listOf(
            order("A", bookedOn = "2026-01-10"),
            order("B", bookedOn = "2026-07-14"),
            order("C", bookedOn = "2026-03-02"),
            order("D", bookedOn = "2026-05-20"),
        )
        assertEquals(listOf("B", "D", "C"), orders.latestAwaitingDelivery(3).refs())
    }

    @Test fun `leaves out delivered and cancelled orders`() {
        val orders = listOf(
            order("A", status = "DELIVERED", bookedOn = "2026-07-01"),
            order("B", status = "CANCELLED", bookedOn = "2026-07-02"),
            order("C", bookedOn = "2026-06-01"),
        )
        assertEquals(listOf("C"), orders.latestAwaitingDelivery(3).refs())
    }

    @Test fun `orders without a booking day come last`() {
        val orders = listOf(order("A"), order("B", bookedOn = "2026-02-01"))
        assertEquals(listOf("B", "A"), orders.latestAwaitingDelivery(3).refs())
    }
}
