package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.vin.VinDecoding
import app.tostrr.domain.vin.VinSection
import app.tostrr.domain.vin.VinSegment
import app.tostrr.ui.theme.TostrrColors

/** One part of the VIN: its positions and code on the left, what it is and what it means on the right. */
@Composable
internal fun VinSegmentRow(segment: VinSegment, decoding: VinDecoding) {
    val positions = segment.section.positions
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(Modifier.width(80.dp)) {
            Text(
                if (positions.first == positions.last) "${positions.first}" else "${positions.first}-${positions.last}",
                style = MaterialTheme.typography.labelSmall,
                color = TostrrColors.Muted,
            )
            Text(
                segment.code,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TostrrColors.Accent,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(segment.section.labelRes), style = MaterialTheme.typography.labelMedium, color = TostrrColors.Muted)
            Text(meaning(segment, decoding), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun meaning(segment: VinSegment, decoding: VinDecoding): String = when (segment.section) {
    VinSection.CHECK_DIGIT -> when {
        decoding.checkDigitValid -> stringResource(R.string.detail_vin_check_valid)
        decoding.checkDigitRequired -> stringResource(R.string.detail_vin_check_invalid, decoding.expectedCheckDigit.toString())
        else -> stringResource(R.string.detail_vin_check_not_required)
    }
    VinSection.SERIAL -> segment.code
    else -> {
        val language = LocalConfiguration.current.locales[0].language
        segment.meaning?.values?.let { it[language] ?: it["en"] } ?: stringResource(R.string.detail_vin_unknown)
    }
}
