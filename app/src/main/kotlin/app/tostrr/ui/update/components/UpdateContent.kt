package app.tostrr.ui.update.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.domain.update.AppUpdate
import app.tostrr.ui.common.TostrrLogo
import app.tostrr.ui.update.UpdateViewModel.Download

@Composable
internal fun UpdateContent(
    update: AppUpdate,
    download: Download,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (download is Download.Failed) {
                    Text(
                        stringResource(R.string.update_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (download is Download.Running) {
                    Text(
                        stringResource(R.string.update_downloading, (download.progress * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                } else {
                    Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                when (download) {
                                    Download.Done -> R.string.update_install
                                    Download.Failed -> R.string.update_retry
                                    else -> R.string.update_download
                                },
                            ),
                        )
                    }
                }
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.update_later))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            TostrrLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.update_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    R.string.update_version,
                    update.version,
                    Formatter.formatShortFileSize(LocalContext.current, update.sizeBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            ReleaseNotesText(update.notes)
        }
    }
}
