package app.tostrr.ui.update

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.tostrr.ui.architecture.OnSideEffect
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.update.UpdateViewModel.Action
import app.tostrr.ui.update.UpdateViewModel.SideEffect
import app.tostrr.ui.update.components.UpdateContent
import org.koin.androidx.compose.koinViewModel
import java.io.File

/** What the new release brings, with the buttons to install it or leave it for later. */
@Composable
fun UpdateScreen(onClose: () -> Unit, viewModel: UpdateViewModel = koinViewModel()) {
    val state by viewModel.watchAsState()
    val context = LocalContext.current

    OnSideEffect(viewModel) { effect ->
        when (effect) {
            is SideEffect.OpenInstaller -> context.startActivity(installerIntent(context, effect.path))
            SideEffect.Close -> onClose()
        }
    }
    LaunchedEffect(state.update) {
        if (state.update == null) onClose()
    }

    state.update?.let { update ->
        UpdateContent(
            update,
            state.download,
            onUpdate = { viewModel.handle(Action.OnUpdate) },
            onLater = { viewModel.handle(Action.OnLater) },
        )
    }
}

/** Asks the user to allow installs from Tostrr the first time; Android refuses an APK not signed with the app's key. */
private fun installerIntent(context: Context, path: String): Intent = Intent(Intent.ACTION_VIEW)
    .setDataAndType(
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path)),
        "application/vnd.android.package-archive",
    )
    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
