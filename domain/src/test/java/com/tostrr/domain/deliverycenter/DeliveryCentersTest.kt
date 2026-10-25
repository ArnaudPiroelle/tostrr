package com.tostrr.domain.deliverycenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeliveryCentersTest {
    private fun center(name: String, country: String, address: String = "1 rue X") =
        DeliveryCenter(name, country, "Ville", address)

    private val maurepas = center("Centre de Livraison Tesla Paris-Ouest", "FR", "5-7 Av. Louis Pasteur, 78310 Maurepas")
    private val richmondUs = center("Richmond", "US")
    private val richmondCa = center("Richmond", "CA")
    private val centers = DeliveryCenters(listOf(maurepas, richmondUs, richmondCa))

    @Test
    fun `finds the centre by the name Tesla gives, ignoring case accents and spacing`() {
        assertEquals(maurepas, centers.find("Centre de Livraison Tesla Paris-Ouest", "FR"))
        assertEquals(maurepas, centers.find("  centre de livraison   tesla PARIS-OUEST ", null))
        assertEquals(center("Mérida", "MX"), DeliveryCenters(listOf(center("Mérida", "MX"))).find("Merida", "MX"))
    }

    @Test
    fun `the order country picks between centres sharing a name`() {
        assertEquals(richmondCa, centers.find("Richmond", "CA"))
        assertEquals(richmondUs, centers.find("Richmond", "us"))
        // Ambiguous without a country: better no address than the wrong one.
        assertNull(centers.find("Richmond", null))
    }

    @Test
    fun `unknown or missing names find nothing`() {
        assertNull(centers.find("Centre Tesla Inconnu", "FR"))
        assertNull(centers.find(null, "FR"))
    }

    @Test
    fun `maps link searches the name and address, or the name alone`() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=Centre+de+Livraison+Tesla+Paris-Ouest%2C+5-7+Av.+Louis+Pasteur%2C+78310+Maurepas",
            googleMapsUrl("Centre de Livraison Tesla Paris-Ouest", maurepas),
        )
        assertEquals("https://www.google.com/maps/search/?api=1&query=Tesla+Nulle+Part", googleMapsUrl("Tesla Nulle Part", null))
    }
}
