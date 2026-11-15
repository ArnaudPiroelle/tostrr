package app.tostrr.data.catalog

import app.tostrr.domain.order.OptionInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OptionCatalogTest {
    private val catalog = OptionCatalog(RuntimeEnvironment.getApplication())

    @Test fun `the bundled labels are known from the start`() {
        assertEquals(OptionInfo("Pearl White Multi-Coat", "paints"), catalog.codes.value["PPSW"])
    }

    @Test fun `every code is an upper-case code with a label`() {
        assertTrue(catalog.codes.value.size > 500)
        catalog.codes.value.forEach { (code, info) ->
            assertEquals(code.trim().uppercase(), code)
            assertTrue(code, info.label.isNotBlank())
        }
    }
}
