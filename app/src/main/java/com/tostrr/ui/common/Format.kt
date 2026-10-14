package com.tostrr.ui.common

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatDateTime(epochMs: Long): String =
    if (epochMs <= 0) "—"
    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(epochMs))
