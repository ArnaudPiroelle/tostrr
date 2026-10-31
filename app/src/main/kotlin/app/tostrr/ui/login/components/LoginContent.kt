package app.tostrr.ui.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tostrr.R
import app.tostrr.ui.common.TostrrLogo
import app.tostrr.ui.theme.TostrrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginContent(
    busy: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onPasteRedirect: (String) -> Unit,
) {
    var showPaste by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        // At least the screen's height, so the buttons sit at the bottom; scrolls on small or zoomed screens.
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                TostrrLogo(Modifier.size(56.dp, 64.dp))
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.app_name), fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.login_tagline),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = TostrrColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(40.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Feature(Icons.Outlined.Timeline, stringResource(R.string.login_feature_tracking_title), stringResource(R.string.login_feature_tracking_text))
                    Feature(Icons.Outlined.Lock, stringResource(R.string.login_feature_private_title), stringResource(R.string.login_feature_private_text))
                    Feature(Icons.Outlined.Visibility, stringResource(R.string.login_feature_read_only_title), stringResource(R.string.login_feature_read_only_text))
                }
            }

            Column(Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Button(
                    onClick = onLogin,
                    enabled = !busy,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // A fallback for a redirect that should normally just work, so it stays in the background.
                TextButton(onClick = { showPaste = true }) {
                    Text(stringResource(R.string.login_redirect_broken), style = MaterialTheme.typography.bodyMedium, color = TostrrColors.Muted)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.login_promises),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TostrrColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showPaste) {
        ModalBottomSheet(onDismissRequest = { showPaste = false }) {
            PasteRedirect(busy = busy, onSubmit = { showPaste = false; onPasteRedirect(it) })
        }
    }
}
