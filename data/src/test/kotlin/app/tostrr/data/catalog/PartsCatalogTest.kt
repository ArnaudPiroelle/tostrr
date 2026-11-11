package app.tostrr.data.catalog

import app.tostrr.data.testing.FakeHttp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class PartsCatalogTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()
    private fun catalog() = PartsCatalog(http.client, context)

    private val day = LocalDate.of(2026, 11, 12)
    private val noon = day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val hour = 60 * 60 * 1000L

    private fun listed() = http.on(PATH, """{"success":true,"responseObject":[{"id":2,"name":"Model Y","externalReference":"$REF"}]}""")
    private fun notListed() = http.on(PATH, """{"success":false,"message":"Catalog not available for this VIN","responseObject":""}""", code = 400)
    private fun lookedUp() = http.callsTo(PATH).map { it.request.url.queryParameter("vin") }

    @Test fun `a listed vin is built since the day it was found, and kept after a restart`() = runTest {
        listed()
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1), noon)
        assertEquals(PartsCatalog.Listing(day, REF), catalog.listing(VIN1))
        assertEquals(PartsCatalog.Listing(day, REF), catalog().listing(VIN1))
    }

    @Test fun `a found vin is never looked up again`() = runTest {
        listed()
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1), noon)
        catalog.checkOne(listOf(VIN1), noon + 48 * hour)
        assertEquals(listOf(VIN1), lookedUp())
    }

    @Test fun `a vin not listed yet is looked up again an hour later`() = runTest {
        notListed()
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1), noon)
        assertNull(catalog.listing(VIN1))
        catalog.checkOne(listOf(VIN1), noon + hour - 1)
        assertEquals(1, lookedUp().size)
        listed()
        catalog.checkOne(listOf(VIN1), noon + hour)
        assertEquals(day, catalog.listing(VIN1)?.since)
    }

    @Test fun `one vin per call, the one that waited longest`() = runTest {
        notListed()
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1, VIN2), noon)
        catalog.checkOne(listOf(VIN1, VIN2), noon + 2 * hour)
        catalog.checkOne(listOf(VIN1, VIN2), noon + 4 * hour)
        assertEquals(listOf(VIN1, VIN2, VIN1), lookedUp())
    }

    @Test fun `any other answer is no answer, tried again on the next call`() = runTest {
        http.on(PATH, "<html>busy</html>", code = 503)
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1), noon)
        http.failOn(PATH)
        catalog.checkOne(listOf(VIN1), noon + 1)
        listed()
        catalog.checkOne(listOf(VIN1), noon + 2)
        assertEquals(3, lookedUp().size)
        assertEquals(day, catalog.listing(VIN1)?.since)
    }

    @Test fun `clearing forgets every vin, on disk too`() = runTest {
        listed()
        val catalog = catalog()
        catalog.checkOne(listOf(VIN1), noon)
        catalog.clear()
        assertNull(catalog.listing(VIN1))
        assertNull(catalog().listing(VIN1))
    }

    @Test fun `a vin found without its reference is looked up again and keeps its day`() = runTest {
        File(context.filesDir, "parts_catalog.json").writeText("""{"$VIN1":{"c":$noon,"s":"2026-11-02"}}""")
        listed()
        val catalog = catalog()
        assertEquals(PartsCatalog.Listing(LocalDate.of(2026, 11, 2), null), catalog.listing(VIN1))
        catalog.checkOne(listOf(VIN1), noon + hour)
        assertEquals(PartsCatalog.Listing(LocalDate.of(2026, 11, 2), REF), catalog.listing(VIN1))
        catalog.checkOne(listOf(VIN1), noon + 48 * hour)
        assertEquals(1, lookedUp().size)
    }

    private companion object {
        const val PATH = "/api/catalogs"
        const val VIN1 = "LRWYGCEK1TC000001"
        const val VIN2 = "LRWYGCEK1TC000002"
        const val REF = "47b31c21-0000-4000-8000-000000000001"
    }
}
