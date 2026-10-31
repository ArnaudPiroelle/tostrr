package app.tostrr.domain.order

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class LocalDateFormatsTest {
    private val day = LocalDate.of(2026, 11, 30)
    private val appointment = LocalDateTime.of(2026, 11, 30, 14, 30)

    @Test fun `french puts the day first`() {
        assertEquals("30/11/26", day.format(LocalDateFormats.date(Locale.FRANCE)))
        assertEquals("30/11/2026 14:30", appointment.format(LocalDateFormats.dateTime(Locale.FRANCE)))
    }

    @Test fun `english puts the month first, with a 12-hour clock`() {
        assertEquals("11/30/26", day.format(LocalDateFormats.date(Locale.US)))
        assertEquals("11/30/2026 2:30 PM", appointment.format(LocalDateFormats.dateTime(Locale.US)))
    }
}

class MoneyFormatTest {
    @Test fun `the currency comes from Tesla, the way of writing it from the device`() {
        assertEquals("5 000,50 €", OrderInfoMapper.formatMoney(5000.5, "EUR", Locale.FRANCE).spaces())
        assertEquals("€5,000.50", OrderInfoMapper.formatMoney(5000.5, "EUR", Locale.US))
        assertEquals("$5,000", OrderInfoMapper.formatMoney(5000.0, "USD", Locale.US))
    }

    /** French uses narrow no-break spaces. */
    private fun String.spaces() = replace(' ', ' ').replace(' ', ' ')
}
