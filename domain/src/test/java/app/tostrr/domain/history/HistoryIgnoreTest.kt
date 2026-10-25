package app.tostrr.domain.history

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
    fun realChangeNextToIgnoredOneIsStillReported() {
        val old = """{"details":{"tasks":{"scheduling":{"card":{"title":"Hello"},"apptDateTimeAddressStr":"a"}}}}"""
        val new = """{"details":{"tasks":{"scheduling":{"card":{"title":"Bonjour"},"apptDateTimeAddressStr":"b"}}}}"""
        assertEquals(listOf("details.tasks.scheduling.apptDateTimeAddressStr"), changed(old, new))
    }
}
