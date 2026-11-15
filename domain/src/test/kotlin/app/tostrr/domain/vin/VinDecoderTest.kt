package app.tostrr.domain.vin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VinDecoderTest {
    private fun code(section: VinSection, code: String, en: String) = VinCode(section, code, mapOf("en" to en))

    private val table = VinCodeTable(
        "2026-11-15",
        listOf(
            code(VinSection.MANUFACTURER, "LRW", "Tesla, China"),
            code(VinSection.MODEL, "3", "Model 3"),
            code(VinSection.BODY, "E", "Sedan"),
            code(VinSection.BATTERY, "E", "Lithium-ion"),
            code(VinSection.MODEL_YEAR, "P", "2023"),
            code(VinSection.FACTORY, "C", "Shanghai"),
        ),
    )

    private fun VinDecoding.meaning(section: VinSection) = segments.single { it.section == section }.meaning?.values?.get("en")

    @Test fun `a VIN is cut into its ten parts, in order`() {
        val decoding = VinDecoder.decode("LRW3E7EB5PC000003", table)!!

        assertEquals(VinSection.entries, decoding.segments.map { it.section })
        assertEquals(
            listOf("LRW", "3", "E", "7", "E", "B", "5", "P", "C", "000003"),
            decoding.segments.map { it.code },
        )
        assertEquals("Tesla, China", decoding.meaning(VinSection.MANUFACTURER))
        assertEquals("2023", decoding.meaning(VinSection.MODEL_YEAR))
        assertEquals("Shanghai", decoding.meaning(VinSection.FACTORY))
        assertEquals("2026-11-15", decoding.tableUpdated)
    }

    @Test fun `a code the table doesn't know, and the computed parts, have no meaning`() {
        val decoding = VinDecoder.decode("LRW3E7EB5PC000003", table)!!

        assertNull(decoding.meaning(VinSection.MOTOR))
        assertNull(decoding.meaning(VinSection.CHECK_DIGIT))
        assertNull(decoding.meaning(VinSection.SERIAL))
    }

    @Test fun `spaces and lower case are tolerated, anything else that isn't 17 VIN characters is refused`() {
        assertEquals("LRW3E7EB5PC000003", VinDecoder.decode(" lrw3e7eb5pc 000003 ", table)?.vin)
        assertNull(VinDecoder.decode("LRW3E7EB5PC00000", table))
        assertNull(VinDecoder.decode("LRW3E7EB5PC0000031", table))
        assertNull(VinDecoder.decode("LRW3E7EB5PC00000O", table))
        assertNull(VinDecoder.decode("LRW3I7EB5PC000003", table))
        assertNull(VinDecoder.decode("N/A", table))
    }

    @Test fun `the check digit follows ISO 3779`() {
        mapOf(
            "11111111111111111" to '1',
            "1M8GDM9AXKP042788" to 'X',
            "5YJ3E1EA9PF000001" to '9',
            "7SAYGDEE1RA000002" to '1',
            "LRW3E7EB5PC000003" to '5',
            "XP7YGCEKXSB000004" to 'X',
            "5YJ3E1EA7LF123456" to '4',
        ).forEach { (vin, expected) -> assertEquals(vin, expected, VinCheckDigit.expected(vin)) }
    }

    @Test fun `only North American VINs must have a valid check digit`() {
        val invalidUs = VinDecoder.decode("5YJ3E1EA7LF123456", table)!!
        assertFalse(invalidUs.checkDigitValid)
        assertTrue(invalidUs.checkDigitRequired)
        assertEquals('4', invalidUs.expectedCheckDigit)

        val shanghai = VinDecoder.decode("LRW3E7EU8VC052692", table)!!
        assertFalse(shanghai.checkDigitRequired)
        assertTrue(VinDecoder.decode("LRW3E7EB5PC000003", table)!!.checkDigitValid)
    }
}
