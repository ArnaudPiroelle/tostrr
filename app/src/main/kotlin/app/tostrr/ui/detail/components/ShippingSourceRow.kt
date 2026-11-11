package app.tostrr.ui.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.theme.TostrrColors

/** Opens the community ship tracking site the voyage comes from. */
@Composable
internal fun ShippingSourceRow() {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { runCatching { uriHandler.openUri(SOURCE_URL) } }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TostrrColors.Accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.detail_shipping_open_source), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Accent)
    }
}

private const val SOURCE_URL = "https://tff-ship-tracking.pages.dev/"
