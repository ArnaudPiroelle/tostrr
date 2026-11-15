package app.tostrr.domain.vin

/**
 * The parts of a Tesla VIN, in the order they come, with the positions (from 1 to 17) they cover. The sections
 * read from the [VinCodeTable] have an [id], the name the table gives them; the check digit and the serial number
 * are read from the VIN itself.
 */
enum class VinSection(val positions: IntRange, val id: String?) {
    MANUFACTURER(1..3, "manufacturer"),
    MODEL(4..4, "model"),
    BODY(5..5, "body"),
    RESTRAINTS(6..6, "restraints"),
    BATTERY(7..7, "battery"),
    MOTOR(8..8, "motor"),
    CHECK_DIGIT(9..9, null),
    MODEL_YEAR(10..10, "model_year"),
    FACTORY(11..11, "factory"),
    SERIAL(12..17, null),
    ;

    companion object {
        fun ofId(id: String): VinSection? = entries.firstOrNull { it.id != null && it.id == id }
    }
}
