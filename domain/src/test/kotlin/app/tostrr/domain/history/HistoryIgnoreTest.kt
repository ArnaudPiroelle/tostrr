package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryIgnoreTest {
    private fun changed(old: String, new: String) =
        JsonDiff.diff(HistoryIgnore.prune(Json.parseToJsonElement(old)), HistoryIgnore.prune(Json.parseToJsonElement(new))).map { it.path }

    @Test
    fun cardTextsAreIgnored() {
        assertTrue(HistoryIgnore.isIgnored("details.tasks.scheduling.card.title"))
        assertTrue(HistoryIgnore.isIgnored("details.tasks.scheduling.card"))
        assertTrue(HistoryIgnore.isIgnored("internal.signals.x"))
        assertTrue(HistoryIgnore.isIgnored("details.tasks.tradeIn.isMatched"))
    }

    @Test
    fun suffixAndContainsPatternsAreIgnored() {
        assertTrue(HistoryIgnore.isIgnored("details.tasks.registration.strings"))
        assertTrue(HistoryIgnore.isIgnored("details.tasks.x.strings.title"))
        assertTrue(HistoryIgnore.isIgnored("details.tasks.x.screen.description"))
        assertTrue(HistoryIgnore.isIgnored("order.something.order"))
    }

    @Test
    fun meaningfulFieldsAreKept() {
        assertFalse(HistoryIgnore.isIgnored("details.tasks.scheduling.apptDateTimeAddressStr"))
        assertFalse(HistoryIgnore.isIgnored("order.orderStatus"))
        assertFalse(HistoryIgnore.isIgnored("details.tasks.registration.orderDetails.vin"))
    }

    @Test
    fun localisedTextChangeProducesNoDiff() {
        val old = """{"details":{"tasks":{"scheduling":{"card":{"title":"Hello"},"apptDateTimeAddressStr":"a"}}}}"""
        val new = """{"details":{"tasks":{"scheduling":{"card":{"title":"Bonjour"},"apptDateTimeAddressStr":"a"}}}}"""
        assertTrue(changed(old, new).isEmpty())
    }

    @Test
    fun tradeInStaffChangesProduceNoDiff() {
        val old = """{"details":{"tasks":{"tradeIn":{"status":"OPEN","assignedTo":"a.b@tesla.com","employeeId":"123","contacts":["c.d@Tesla.com"]}}}}"""
        val new = """{"details":{"tasks":{"tradeIn":{"status":"OFFER","assignedTo":"e.f@tesla.com","employeeId":"456","contacts":["g.h@tesla.com"]}}}}"""
        assertEquals(listOf("details.tasks.tradeIn.status"), changed(old, new))
    }

    @Test
    fun staffDetailsAreOnlyDroppedUnderTheTradeIn() {
        assertTrue(HistoryIgnore.isStaffDetail("details.tasks.tradeIn.advisorEmail", "x@tesla.com"))
        assertTrue(HistoryIgnore.isStaffDetail("details.tasks.tradeIn.assignedEmployee", null))
        assertFalse(HistoryIgnore.isStaffDetail("details.tasks.tradeIn.customerEmail", "me@example.com"))
        assertFalse(HistoryIgnore.isStaffDetail("details.tasks.scheduling.advisorEmail", "x@tesla.com"))
    }

    @Test
    fun savedStaffChangesAreIgnored() {
        assertTrue(HistoryIgnore.ignores(Change(1, "details.tasks.tradeIn.assignedTo", "a@tesla.com", "b@tesla.com")))
        assertTrue(HistoryIgnore.ignores(Change(1, "details.tasks.tradeIn.assignedTo", null, "b@tesla.com")))
        assertFalse(HistoryIgnore.ignores(Change(1, "details.tasks.tradeIn.status", "OPEN", "OFFER")))
    }

    @Test
    fun realChangeNextToIgnoredOneIsStillReported() {
        val old = """{"details":{"tasks":{"scheduling":{"card":{"title":"Hello"},"apptDateTimeAddressStr":"a"}}}}"""
        val new = """{"details":{"tasks":{"scheduling":{"card":{"title":"Bonjour"},"apptDateTimeAddressStr":"b"}}}}"""
        assertEquals(listOf("details.tasks.scheduling.apptDateTimeAddressStr"), changed(old, new))
    }
}
