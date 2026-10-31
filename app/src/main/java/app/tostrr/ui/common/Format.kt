package app.tostrr.ui.common

import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

fun formatDateTime(epochMs: Long): String =
    if (epochMs <= 0) "—"
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(epochMs))

/** `28/08/2026` in French, `8/28/26` in English: the device's short date, in its time zone. */
fun formatShortDate(epochMs: Long): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))

/** `12:12` in French, `12:12 PM` in English, in the device time zone. */
fun formatShortTime(epochMs: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
