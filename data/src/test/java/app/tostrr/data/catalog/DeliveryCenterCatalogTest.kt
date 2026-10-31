package app.tostrr.data.catalog

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeliveryCenterCatalogTest {
    private val catalog = DeliveryCenterCatalog(RuntimeEnvironment.getApplication())

    @Test fun `the bundled centres are read, whatever the name's case and accents`() = runTest {
        val center = catalog.centers().find("centre de livraison tesla paris-ouest", "FR")!!
        assertEquals("Maurepas", center.city)
        assertEquals("5-7 Av. Louis Pasteur, 78310 Maurepas", center.address)
    }

    @Test fun `the country tells apart centres of the same name`() = runTest {
        assertEquals("CA", catalog.centers().find("Richmond", "CA")?.country)
        assertEquals("Glen Allen", catalog.centers().find("Richmond", "US")?.city)
        assertNull(catalog.centers().find("Richmond", null))
    }

    @Test fun `an unknown centre is not found`() = runTest {
        assertNull(catalog.centers().find("Tesla Atlantis", "FR"))
    }

    @Test fun `the asset is read once`() = runTest {
        assertSame(catalog.centers(), catalog.centers())
    }
}
