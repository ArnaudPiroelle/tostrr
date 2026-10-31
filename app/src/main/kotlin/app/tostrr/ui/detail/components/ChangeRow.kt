package app.tostrr.ui.detail.components

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.Change
import app.tostrr.ui.common.formatShortDate
import app.tostrr.ui.common.formatShortTime
import app.tostrr.ui.theme.TostrrColors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** One field-level change between two syncs. A missing old value means the field appeared, a missing new one that it vanished. */
@Composable
internal fun ChangeRow(change: Change) {
    val added = change.oldValue == null
    val removed = change.newValue == null
    val positive = TostrrColors.Positive
    val negative = TostrrColors.Negative
    val muted = TostrrColors.Muted
    var showDetail by rememberSaveable { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(74.dp)) {
            Text(formatShortDate(change.timestampEpochMs), style = MaterialTheme.typography.labelSmall, color = muted)
            Text(formatShortTime(change.timestampEpochMs), style = MaterialTheme.typography.labelSmall, color = muted)
        }
        Icon(
            imageVector = when {
                added -> Icons.Filled.Add
                removed -> Icons.Filled.Remove
                else -> Icons.Filled.SwapHoriz
            },
            contentDescription = when {
                added -> stringResource(R.string.detail_change_added)
                removed -> stringResource(R.string.detail_change_removed)
                else -> stringResource(R.string.detail_change_modified)
            },
            modifier = Modifier.size(16.dp),
            tint = muted,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                breakablePath(change.key),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            // Objects and arrays are summarised ("{9 champs}", "{}") and inspected through the Détail button.
            // Remembered: a structured side can hold up to 120 kB of JSON, too much to parse on every recomposition.
            val resources = LocalContext.current.resources
            val oldText = remember(change) { change.oldValue?.let { summarize(resources, it, change.structured) } }
            val newText = remember(change) { change.newValue?.let { summarize(resources, it, change.structured) } }
            when {
                // A scalar that vanished: the key alone says it all. A vanished container shows what it held.
                removed -> if (change.structured) ValueLine(oldText.orEmpty(), negative)
                added -> newText?.takeIf { it != "∅" }?.let { ValueLine(it, positive) }
                // Old and new on separate lines: "old → new" on one line wraps mid-value on a phone.
                else -> Column(Modifier.padding(top = 2.dp)) {
                    Text(
                        oldText.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = negative,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = muted)) { append("→ ") }
                            withStyle(SpanStyle(color = positive)) { append(newText.orEmpty()) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (change.structured) {
                // Small pill, same shape language as the status badge in the hero.
                AssistChip(
                    onClick = { showDetail = true },
                    // labelLarge is sans-serif; labelMedium is monospace in this theme and makes the pill look like code.
                    label = { Text(stringResource(R.string.detail_change_details), style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = { Icon(Icons.Filled.DataObject, contentDescription = null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(50),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(top = 8.dp).height(30.dp),
                )
            }
        }
    }
    if (showDetail) ChangeDetailSheet(change) { showDetail = false }
}

/** Short human summary of a change side: scalars as-is, containers as "{n champs}" / "[n éléments]" or "{}" / "[]". */
private fun summarize(resources: Resources, value: String, structured: Boolean): String {
    if (!structured) return value.ifEmpty { "∅" }
    return when (val el = runCatching { Json.parseToJsonElement(value) }.getOrNull()) {
        is JsonObject -> if (el.isEmpty()) "{}" else resources.getString(R.string.detail_change_fields, el.size)
        is JsonArray -> if (el.isEmpty()) "[]" else resources.getString(R.string.detail_change_items, el.size)
        // Scalar side of a structured change, or a container too large to have been stored whole.
        else -> when {
            value.isEmpty() -> "∅"
            value.startsWith("{") -> "{…}"
            value.startsWith("[") -> "[…]"
            else -> value
        }
    }
}
