package app.tostrr.ui.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tostrr.R
import app.tostrr.ui.theme.TostrrColors

/** Fallback when the `tesla://` redirect doesn't reach the app: the user pastes the URL the browser stopped on. */
@Composable
internal fun PasteRedirect(busy: Boolean, onSubmit: (String) -> Unit) {
    var pasted by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
        Text(stringResource(R.string.login_paste_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_paste_text),
            style = MaterialTheme.typography.bodyMedium,
            color = TostrrColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.login_paste_placeholder)) },
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(pasted) },
            enabled = pasted.isNotBlank() && !busy,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.login_paste_submit)) }
    }
}
