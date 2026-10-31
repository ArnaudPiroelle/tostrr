package app.tostrr.ui.detail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.order.OptionInfo
import app.tostrr.domain.order.Order
import app.tostrr.ui.theme.TostrrColors

@Composable
internal fun ConfigurationContent(order: Order, options: Map<String, OptionInfo>) {
    val codes = order.visibleOptionCodes
    if (codes.isEmpty()) return Empty(stringResource(R.string.detail_configuration_empty))
    // Light list: description first, code as a quiet tag on the right. No inner frame or dividers.
    codes.forEach { code ->
        val info = options[code]
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
            Text(
                info?.label ?: stringResource(R.string.detail_configuration_no_label),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (info != null) TostrrColors.OnSurface else TostrrColors.Muted,
            )
            Text(
                code,
                Modifier.padding(start = 12.dp, top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TostrrColors.Muted,
            )
        }
    }
}
