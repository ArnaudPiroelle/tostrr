package app.tostrr.data.catalog

import app.tostrr.domain.vin.VinDecoder
import app.tostrr.domain.vin.VinSection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class VinCodeCatalogTest {
    private val catalog = VinCodeCatalog(RuntimeEnvironment.getApplication())

    @Test fun `the bundled table decodes a Shanghai-built Model 3`() = runTest {
        val decoding = VinDecoder.decode("LRW3E7EB5PC000003", catalog.table())!!
        val meanings = decoding.segments.associate { it.section to it.meaning?.values?.get("en") }

        assertEquals("Tesla, China", meanings[VinSection.MANUFACTURER])
        assertEquals("Model 3", meanings[VinSection.MODEL])
        assertEquals("Sedan 4-door / LHD (Model 3); Class 2H pickup / LHD (Cybertruck)", meanings[VinSection.BODY])
        assertEquals("2023", meanings[VinSection.MODEL_YEAR])
        assertEquals("Shanghai (China)", meanings[VinSection.FACTORY])
    }

    @Test fun `every entry is worded in French too`() = runTest {
        catalog.table().codes.forEach { assertEquals(it.toString(), setOf("en", "fr"), it.values.keys) }
    }

    @Test fun `a code the table doesn't know has no meaning`() = runTest {
        val decoding = VinDecoder.decode("LRW3E7EU8VC052692", catalog.table())!!

        assertNull(decoding.segments.single { it.section == VinSection.MOTOR }.meaning)
    }

    @Test fun `the asset is read once`() = runTest {
        assertSame(catalog.table(), catalog.table())
    }
}
