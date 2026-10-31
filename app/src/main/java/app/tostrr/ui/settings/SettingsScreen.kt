package app.tostrr.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tostrr.domain.auth.TeslaAccount
import app.tostrr.domain.sync.SyncInterval
import app.tostrr.domain.sync.SyncSettings
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.settings.SettingsViewModel.Action
import app.tostrr.ui.theme.TostrrColors
import org.koin.androidx.compose.koinViewModel
import java.io.File

/** The app settings, grouped in sections; add a section as one more item of the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.watchAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Réglages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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

@Composable
private fun SyncSection(
    sync: SyncSettings,
    onAutoSyncChange: (Boolean) -> Unit,
    onIntervalChange: (SyncInterval) -> Unit,
) {
    SettingsSection("Actualisation automatique") {
        SwitchRow(
            title = "Actualiser en arrière-plan",
            summary = if (sync.autoSync) {
                "Les commandes sont récupérées régulièrement, même app fermée."
            } else {
                "Seulement quand vous actualisez. Widgets et notifications figés."
            },
            checked = sync.autoSync,
            onCheckedChange = onAutoSyncChange,
        )
        HorizontalDivider(color = TostrrColors.Border)
        // Always shown, so the screen doesn't change shape; only usable while auto sync is on.
        ChoiceRow(
            title = "Fréquence",
            summary = "Délai entre deux récupérations.",
            selected = sync.interval,
            options = SyncInterval.entries,
            label = { it.label() },
            enabled = sync.autoSync,
            onSelect = onIntervalChange,
        )
    }
}

@Composable
private fun AccountSection(account: TeslaAccount?, onLogout: () -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    SettingsSection("Compte Tesla") {
        // Who is logged in, when Tesla gave the account's name.
        if (account != null) {
            ListItem(
                overlineContent = { Text("Connecté en tant que", style = MaterialTheme.typography.bodySmall, color = TostrrColors.Muted) },
                headlineContent = { Text(account.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Avatar(account.avatarPath) },
                colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
            )
        }
        // An action, not a setting: a plain button.
        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = if (account != null) 0.dp else 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Se déconnecter")
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = TostrrColors.SurfaceHigh,
            title = { Text("Se déconnecter ?") },
            text = {
                Text(
                    "Vos commandes en cache et l'historique des modifications seront supprimés de cet appareil.",
                    color = TostrrColors.Muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) { Text("Se déconnecter", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Annuler") } },
        )
    }
}

/** The account picture from its local copy, round; the generic icon while it loads, or when there is none. */
@Composable
private fun Avatar(path: String?) {
    val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
    // Only the icon is tinted, never the photo.
    var loaded by remember(path) { mutableStateOf(false) }
    AsyncImage(
        model = path?.let(::File),
        contentDescription = null,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        onLoading = { loaded = false },
        onSuccess = { loaded = true },
        onError = { loaded = false },
        contentScale = ContentScale.Crop,
        colorFilter = if (loaded) null else ColorFilter.tint(TostrrColors.Muted),
        modifier = Modifier.size(40.dp).clip(CircleShape),
    )
}

/** A titled group of settings rows. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = TostrrColors.Accent,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TostrrColors.Surface),
        ) { Column(content = content) }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        // Descriptions stay short: two lines at most.
        supportingContent = { Text(summary, color = TostrrColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        // The whole row toggles; the switch only shows the state.
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/**
 * A setting with a few possible values: shows its [summary] and the selected value, and a tap opens a dialog to pick
 * another. Greyed out and inert when not [enabled].
 */
@Composable
private fun <T> ChoiceRow(
    title: String,
    summary: String,
    selected: T,
    options: List<T>,
    label: (T) -> String,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val content = if (enabled) TostrrColors.OnSurface else TostrrColors.Muted
    ListItem(
        headlineContent = { Text(title, color = content) },
        supportingContent = {
            Column {
                Text(summary, color = TostrrColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // The current value, under the description as in the system settings.
                Text(
                    label(selected),
                    Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) TostrrColors.Accent else TostrrColors.Muted,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = TostrrColors.Surface),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { choosing = true },
    )

    if (choosing && enabled) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            containerColor = TostrrColors.SurfaceHigh,
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        choosing = false
                                        onSelect(option)
                                    },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == selected, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = false }) { Text("Annuler") } },
        )
    }
}

private fun SyncInterval.label(): String = when (this) {
    SyncInterval.MINUTES_15 -> "Toutes les 15 minutes"
    SyncInterval.MINUTES_30 -> "Toutes les 30 minutes"
    SyncInterval.HOUR_1 -> "Toutes les heures"
    SyncInterval.HOURS_2 -> "Toutes les 2 heures"
    SyncInterval.HOURS_6 -> "Toutes les 6 heures"
}
