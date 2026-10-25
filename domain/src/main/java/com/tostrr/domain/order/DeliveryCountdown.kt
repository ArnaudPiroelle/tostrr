package com.tostrr.domain.order

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Where the order stands relative to its delivery, counted in whole days. */
sealed interface DeliveryCountdown {
    /** An appointment is set: [days] until it, 0 on the day, negative once it is past. */
    data class Appointment(val days: Long) : DeliveryCountdown
    /** Only a window is known and it hasn't opened yet: [days] until its first day. */
    data class BeforeWindow(val days: Long) : DeliveryCountdown
    /** Inside the window with no appointment: [days] since its first day (0 on it). */
    data class InWindow(val days: Long) : DeliveryCountdown
    /** The window is over and still no appointment: [days] since its last day. */
    data class PastWindow(val days: Long) : DeliveryCountdown
    data object Unknown : DeliveryCountdown
}

fun Order.deliveryCountdown(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): DeliveryCountdown {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    appointmentDay(zone)?.let { return DeliveryCountdown.Appointment(ChronoUnit.DAYS.between(today, it)) }
    val (start, end) = deliveryWindow?.let { DeliveryWindow.parse(it, today) } ?: return DeliveryCountdown.Unknown
    return when {
        today < start -> DeliveryCountdown.BeforeWindow(ChronoUnit.DAYS.between(today, start))
        today <= end -> DeliveryCountdown.InWindow(ChronoUnit.DAYS.between(start, today))
        else -> DeliveryCountdown.PastWindow(ChronoUnit.DAYS.between(end, today))
    }
}

/** Reads Tesla's `deliveryWindowDisplay`, e.g. "29 Novembre - 29 Décembre", which carries no year. */
internal object DeliveryWindow {

    /** First and last day of the window, placed in the years closest to [today]; null if it can't be read. */
    fun parse(text: String, today: LocalDate): Pair<LocalDate, LocalDate>? {
        val parts = text.split(SEPARATOR).filter { it.isNotBlank() }
        if (parts.size != 2) return null
        val (from, to) = parts.map { part(it) ?: return null }
        val start = closestYear(from.first ?: 1, from.second, today) ?: return null
        var end = dayIn(start.year, to.first, to.second) ?: return null
        if (end < start) end = dayIn(start.year + 1, to.first, to.second) ?: return null
        return start to end
    }

    /** Day of month (null when the part names only a month) and month number. */
    private fun part(text: String): Pair<Int?, Int>? {
        val words = text.trim().split(WHITESPACE)
        val day = words.firstNotNullOfOrNull { it.toIntOrNull() }?.takeIf { it in 1..31 }
        val month = words.firstNotNullOfOrNull(::month) ?: return null
        return day to month
    }

    private fun month(word: String): Int? {
        val key = Normalizer.normalize(word.lowercase(), Normalizer.Form.NFD).filter { it.isLetter() && it.code < 128 }
        return MONTHS.firstOrNull { (prefix, _) -> key.startsWith(prefix) }?.second
    }

    private fun closestYear(day: Int, month: Int, today: LocalDate): LocalDate? =
        (today.year - 1..today.year + 1).mapNotNull { dayIn(it, day, month) }
            .minByOrNull { abs(ChronoUnit.DAYS.between(today, it)) }

    /** A month alone stands for its last day when it closes the window. */
    private fun dayIn(year: Int, day: Int?, month: Int): LocalDate? = runCatching {
        if (day == null) LocalDate.of(year, month, 1).let { it.withDayOfMonth(it.lengthOfMonth()) }
        else MonthDay.of(month, day).atYear(year)
    }.getOrNull()

    private val SEPARATOR = Regex("""\s*[-–]\s*""")
    private val WHITESPACE = Regex("""\s+""")

    /** Prefixes of French and English month names and abbreviations, accents stripped. */
    private val MONTHS = listOf(
        "janv" to 1, "jan" to 1, "fev" to 2, "feb" to 2, "mars" to 3, "mar" to 3, "avr" to 4, "apr" to 4,
        "mai" to 5, "may" to 5, "juin" to 6, "jun" to 6, "juil" to 7, "jul" to 7, "aou" to 8, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )
}
