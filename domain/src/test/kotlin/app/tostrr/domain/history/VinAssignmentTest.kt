package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import app.tostrr.domain.order.Order
import app.tostrr.domain.order.OrderMapper
import app.tostrr.domain.order.StepKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class VinAssignmentTest {
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private fun map(order: String, tasks: String = "{}") =
        OrderMapper.map(obj(order), obj("""{"tasks":$tasks}"""), 0L)

    private fun change(day: Int, key: String, new: String?, hour: Int = 12) = Change(
        LocalDateTime.of(2026, 11, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), key, null, new,
    )

    private fun Order.vinDate(vararg changes: Change) =
        withVinAssignmentDate(changes.toList(), ZoneOffset.UTC).steps.first { it.kind == StepKind.VIN_ASSIGNED }.date

    private val withVin = map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC000001"}""")

    @Test fun `the step shows the day the vin appeared in the history`() {
        assertEquals("03/11/26", withVin.vinDate(change(3, "order.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `the day is the one of the device's time zone`() {
        val date = withVin.withVinAssignmentDate(listOf(change(3, "order.vin", "LRWYGCEK1TC000001", hour = 23)), ZoneOffset.ofHours(1))
            .steps.first { it.kind == StepKind.VIN_ASSIGNED }.date
        assertEquals("04/11/26", date)
    }

    @Test fun `a vin that came inside a new object is found`() {
        val order = map("""{"referenceNumber":"RN1"}""", """{"registration":{"orderDetails":{"vin":"LRWYGCEK1TC000001"}}}""")
        assertEquals("02/11/26", order.vinDate(change(2, "details.tasks.registration", """{"orderDetails":{"vin":"LRWYGCEK1TC000001"}}""")))
    }

    @Test fun `the vin of a swapped car is dated from its own appearance`() {
        val swapped = map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC000002"}""")
        assertEquals("05/11/26", swapped.vinDate(change(5, "order.vin", "LRWYGCEK1TC000002"), change(2, "order.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `a vin that left and came back keeps its first day`() {
        assertEquals("02/11/26", withVin.vinDate(change(5, "order.vin", "LRWYGCEK1TC000001"), change(4, "order.vin", null), change(2, "order.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `the vin showing up elsewhere later keeps its first day`() {
        assertEquals("02/11/26", withVin.vinDate(change(5, "details.tasks.finalPayment.data", """{"vin":"LRWYGCEK1TC000001"}"""), change(2, "order.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `the same text on a path the vin isn't read from doesn't date it`() {
        assertNull(withVin.vinDate(change(5, "details.tasks.insurance.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `a vin the history doesn't date has no date`() {
        assertNull(withVin.vinDate())
    }

    @Test fun `a vin the history doesn't date gets an entry at the sync date`() {
        assertEquals(Change(42L, "order.vin", null, "LRWYGCEK1TC000001"), withVin.undatedVinChange(emptyList(), 42L))
    }

    @Test fun `the entry is on the path the vin is read from`() {
        val order = map("""{"referenceNumber":"RN1"}""", """{"registration":{"orderDetails":{"vin":"LRWYGCEK1TC000001"}}}""")
        assertEquals("details.tasks.registration.orderDetails.vin", order.undatedVinChange(emptyList(), 42L)?.key)
    }

    @Test fun `a dated vin or no vin needs no entry`() {
        assertNull(withVin.undatedVinChange(listOf(change(3, "order.vin", "LRWYGCEK1TC000001")), 42L))
        assertNull(map("""{"referenceNumber":"RN1"}""").undatedVinChange(emptyList(), 42L))
    }

    @Test fun `the vin of a swapped car needs its own entry`() {
        val swapped = map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC000002"}""")
        assertEquals("LRWYGCEK1TC000002", swapped.undatedVinChange(listOf(change(3, "order.vin", "LRWYGCEK1TC000001")), 42L)?.newValue)
    }

    @Test fun `no vin, no date`() {
        assertNull(map("""{"referenceNumber":"RN1"}""").vinDate(change(3, "order.vin", "LRWYGCEK1TC000001")))
    }

    @Test fun `the other steps are left alone`() {
        val order = map("""{"referenceNumber":"RN1","vin":"LRWYGCEK1TC000001"}""", """{"registration":{"orderDetails":{"orderBookedDate":"2026-07-14"}}}""")
        val dated = order.withVinAssignmentDate(listOf(change(3, "order.vin", "LRWYGCEK1TC000001")), ZoneOffset.UTC)
        assertEquals(order.steps.filter { it.kind != StepKind.VIN_ASSIGNED }, dated.steps.filter { it.kind != StepKind.VIN_ASSIGNED })
    }
}
