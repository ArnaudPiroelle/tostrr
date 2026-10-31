package app.tostrr.data.catalog

import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.order.OptionInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OptionCatalogTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()
    private val cacheFile = File(context.filesDir, "option_codes.json")

    private fun catalog() = OptionCatalog(http.client, context)

    private fun serve(body: String, code: Int = 200) = http.on(PATH, body, code)

    private val catalogue = """{"ok":true,"option_codes":[
        {"code":" w38a ","label_en":"18\" Photon Wheels","category":" Wheels "},
        {"code":"PPSW","label_en":"Pearl White"},
        {"code":"NOLABEL"},
        {"label_en":"No code"},
        "not an object"]}"""

    @Test fun `nothing is known before the first download`() {
        assertTrue(catalog().codes.value.isEmpty())
    }

    @Test fun `the downloaded labels are keyed by upper-case code, entries without code or label left out`() = runTest {
        serve(catalogue)
        val catalog = catalog()
        catalog.refreshIfStale()
        assertEquals(
            mapOf("W38A" to OptionInfo("18\" Photon Wheels", "wheels"), "PPSW" to OptionInfo("Pearl White", null)),
            catalog.codes.value,
        )
    }

    @Test fun `the labels are cached for the next start, which doesn't download them again the same day`() = runTest {
        serve(catalogue)
        catalog().refreshIfStale()
        val restarted = catalog()
        assertEquals(setOf("W38A", "PPSW"), restarted.codes.value.keys)
        restarted.refreshIfStale()
        assertEquals(1, http.calls.size)
    }

    @Test fun `a cache older than a day is downloaded again`() = runTest {
        cacheFile.writeText("""{"fetchedAt":0,"codes":{"OLD1":{"l":"Old"}}}""")
        serve(catalogue)
        val catalog = catalog()
        assertEquals(mapOf("OLD1" to OptionInfo("Old", null)), catalog.codes.value)
        catalog.refreshIfStale()
        assertEquals(setOf("W38A", "PPSW"), catalog.codes.value.keys)
    }

    @Test fun `a failed download keeps what was cached and never throws`() = runTest {
        cacheFile.writeText("""{"fetchedAt":0,"codes":{"OLD1":{"l":"Old","c":"paint"}}}""")
        val catalog = catalog()
        val cached = mapOf("OLD1" to OptionInfo("Old", "paint"))

        serve("", code = 503)
        catalog.refreshIfStale()
        assertEquals(cached, catalog.codes.value)

        serve("""{"ok":false}""")
        catalog.refreshIfStale()
        assertEquals(cached, catalog.codes.value)

        serve("""{"ok":true,"option_codes":[]}""")
        catalog.refreshIfStale()
        assertEquals(cached, catalog.codes.value)

        serve("<html>")
        catalog.refreshIfStale()
        http.failOn(PATH)
        catalog.refreshIfStale()
        assertEquals(cached, catalog.codes.value)
    }

    @Test fun `an unreadable cache is ignored`() {
        cacheFile.writeText("{broken")
        assertTrue(catalog().codes.value.isEmpty())
    }

    private companion object {
        const val PATH = "/get/option_codes.php"
    }
}
