package app.tostrr.domain.order

import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Dates as the device's language writes them (`30/11/26` in French, `11/30/26` in English), read at each call so a
 * language change applies to the next sync. The year length is fixed: two digits fit under a progress dot.
 */
internal object LocalDateFormats {

    fun date(locale: Locale = Locale.getDefault(), yearDigits: Int = 2): DateTimeFormatter =
        DateTimeFormatter.ofPattern(datePattern(locale, yearDigits), locale)

    /** Date with a four-digit year, then the time. */
    fun dateTime(locale: Locale = Locale.getDefault()): DateTimeFormatter =
        DateTimeFormatter.ofPattern("${datePattern(locale, 4)} ${timePattern(locale)}", locale)

    private fun datePattern(locale: Locale, yearDigits: Int) =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale)
            .replace(YEAR, "y".repeat(yearDigits))

    private fun timePattern(locale: Locale) =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale)

    private val YEAR = Regex("y+")
}
