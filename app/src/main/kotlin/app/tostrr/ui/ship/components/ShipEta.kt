package app.tostrr.ui.ship.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** `28 nov. 2026`, or `12 nov. 2026 à 16:00` when the site gives the hour, in the device's way. */
@Composable
internal fun shipEta(date: LocalDate, time: LocalTime?): String {
    val day = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    return if (time == null) day else stringResource(R.string.ship_eta_date_time, day, time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
}
