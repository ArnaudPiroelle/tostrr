package app.tostrr.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.settings.SettingsViewModel.Action
import app.tostrr.ui.settings.components.AccountSection
import app.tostrr.ui.settings.components.AppearanceSection
import app.tostrr.ui.settings.components.SyncSection
import org.koin.androidx.compose.koinViewModel

/** The app settings, grouped in sections; add a section as one more item of the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.watchAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { AppearanceSection(state.themeMode, onThemeChange = { viewModel.handle(Action.OnThemeSelected(it)) }) }
            item {
                SyncSection(
                    state.sync,
                    onAutoSyncChange = { viewModel.handle(Action.OnAutoSyncChanged(it)) },
                    onIntervalChange = { viewModel.handle(Action.OnIntervalSelected(it)) },
                )
            }
            item { AccountSection(state.account, onLogout = { viewModel.handle(Action.OnLogout) }) }
        }
    }
}
