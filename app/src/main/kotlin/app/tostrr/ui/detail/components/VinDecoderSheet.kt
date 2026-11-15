package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.vin.VinDecoding
import app.tostrr.ui.theme.TostrrColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** `15 nov. 2026` in French, `Nov 15, 2026` in English; the raw text if it isn't a date. */
private fun formatTableDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
}.getOrDefault(date)

/** The VIN cut into its parts, each with what it means as far as the bundled table knows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VinDecoderSheet(decoding: VinDecoding, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.detail_vin_decoder_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close)) }
        }
        // Ten rows at most: a plain scrolling column keeps them all composed.
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Text(decoding.vin, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
            Text(
                stringResource(R.string.detail_vin_decoder_source, formatTableDate(decoding.tableUpdated)),
                Modifier.padding(top = 4.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TostrrColors.Muted,
            )
            decoding.segments.forEachIndexed { index, segment ->
                if (index > 0) HorizontalDivider()
                VinSegmentRow(segment, decoding)
            }
        }
    }
}
