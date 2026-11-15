package app.tostrr.domain.vin

/** One part of a decoded VIN: its [code] and, when the table knows it, what it means ([meaning]). */
data class VinSegment(val section: VinSection, val code: String, val meaning: VinCode? = null)

/**
 * A VIN cut into its [segments], in order. The check digit (position 9) is checked against the one computed by
 * ISO 3779 ([expectedCheckDigit]), but only North American VINs must have it right ([checkDigitRequired]):
 * Shanghai- or Berlin-built cars may legitimately fail it.
 */
data class VinDecoding(
    val vin: String,
    val segments: List<VinSegment>,
    val expectedCheckDigit: Char,
    val tableUpdated: String,
) {
    val checkDigit: Char get() = vin[8]
    val checkDigitValid: Boolean get() = checkDigit == expectedCheckDigit
    val checkDigitRequired: Boolean get() = vin.take(3) in NORTH_AMERICAN_WMIS

    private companion object {
        /** Tesla's makers' codes for cars built in the United States. */
        val NORTH_AMERICAN_WMIS = setOf("5YJ", "7SA", "7G2")
    }
}

object VinDecoder {
    private val VALID = Regex("[A-HJ-NPR-Z0-9]{17}")

    /** [vin] cut into its segments and decoded with [table], or null when it isn't a VIN (17 characters, no I, O, Q). */
    fun decode(vin: String, table: VinCodeTable): VinDecoding? {
        val normalized = vin.filterNot { it.isWhitespace() }.uppercase()
        if (!VALID.matches(normalized)) return null
        val segments = VinSection.entries.map { section ->
            val code = normalized.substring(section.positions.first - 1, section.positions.last)
            VinSegment(section, code, section.id?.let { table.find(section, code) })
        }
        return VinDecoding(normalized, segments, VinCheckDigit.expected(normalized), table.updated)
    }
}

/** The ISO 3779 check digit (position 9) of a 17-character VIN. */
object VinCheckDigit {
    private val WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)
    private const val LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ"
    private val LETTER_VALUES = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 7, 9, 2, 3, 4, 5, 6, 7, 8, 9)

    fun expected(vin: String): Char {
        val sum = vin.withIndex().sumOf { (index, char) -> value(char) * WEIGHTS[index] }
        val remainder = sum % 11
        return if (remainder == 10) 'X' else '0' + remainder
    }

    private fun value(char: Char): Int = if (char.isDigit()) char - '0' else LETTER_VALUES[LETTERS.indexOf(char)]
}
