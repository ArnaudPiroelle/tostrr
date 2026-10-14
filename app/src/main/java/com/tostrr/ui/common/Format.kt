package com.tostrr.ui.common

import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun formatDateTime(epochMs: Long): String =
    if (epochMs <= 0) "—"
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(epochMs))

private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
private val shortTime = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

/** `28/08/2026`, in the device time zone. */
fun formatShortDate(epochMs: Long): String = shortDate.format(Instant.ofEpochMilli(epochMs))

/** `12:12`, in the device time zone. */
fun formatShortTime(epochMs: Long): String = shortTime.format(Instant.ofEpochMilli(epochMs))
