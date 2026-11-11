package app.tostrr.domain.history

import app.tostrr.domain.order.Change
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFilterTest {
    private val vin = Change(3L, "order.vin", null, "LRWYGCEK1TC000001")
    private val contract = Change(2L, "details.tasks.agreements.eSignStatus", "COMPLETE", "VOIDED")
    private val insurance = Change(1L, "details.tasks.insurance.hasVin", "true", "false")
    private val history = listOf(vin, contract, insurance)

    @Test fun `the default shows everything`() {
        assertEquals(history, HistoryFilter().apply(history))
    }

    @Test fun `important only hides the minor changes, and their category`() {
        val filter = HistoryFilter(importantOnly = true)
        assertEquals(listOf(vin, contract), filter.apply(history))
        assertEquals(mapOf(ChangeCategory.VIN to 1, ChangeCategory.AGREEMENTS to 1), filter.categoryCounts(history))
        assertEquals(3, HistoryFilter().categoryCounts(history).values.sum())
    }

    @Test fun `a category keeps only its changes`() {
        assertEquals(listOf(contract), HistoryFilter(category = ChangeCategory.AGREEMENTS).apply(history))
    }

    @Test fun `the counts follow the categories' order`() {
        assertEquals(listOf(ChangeCategory.VIN, ChangeCategory.AGREEMENTS, ChangeCategory.INSURANCE), HistoryFilter().categoryCounts(history).keys.toList())
    }
}
