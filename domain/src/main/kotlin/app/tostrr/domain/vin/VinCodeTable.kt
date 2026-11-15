package app.tostrr.domain.vin

/**
 * What [code] means at [section]'s positions, worded per language ([values], keyed by language code, `en` always
 * there). The table is flat: a code that means different things depending on the model says so in its text.
 */
data class VinCode(val section: VinSection, val code: String, val values: Map<String, String>)

/** The meaning of each Tesla VIN code the app knows, as of [updated] (`yyyy-MM-dd`). */
data class VinCodeTable(val updated: String, val codes: List<VinCode>) {
    private val bySection = codes.associateBy { it.section to it.code }

    fun find(section: VinSection, code: String): VinCode? = bySection[section to code]
}
