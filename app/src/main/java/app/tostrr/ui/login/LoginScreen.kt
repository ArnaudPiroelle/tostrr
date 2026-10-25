package app.tostrr.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tostrr.ui.theme.TostrrColors

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onPasteRedirect: (String) -> Unit,
) {
    var showPaste by rememberSaveable { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Tostrr", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Suivez l'avancement de vos commandes Tesla.",
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        Button(onClick = onLogin, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TostrrColors.OnSurface)
            else Text("Se connecter avec Tesla")
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { showPaste = !showPaste }) {
            Text("La redirection ne fonctionne pas ?", color = TostrrColors.Muted)
        }
        if (showPaste) {
            Text(
                "Après connexion, la page « introuvable » affiche une URL commençant par tesla://auth/callback. Collez-la ici.",
                style = MaterialTheme.typography.bodyMedium,
                color = TostrrColors.Muted,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pasted,
                onValueChange = { pasted = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("tesla://auth/callback?code=…") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onPasteRedirect(pasted) }, enabled = pasted.isNotBlank() && !busy) { Text("Valider") }
        }
    }
}
