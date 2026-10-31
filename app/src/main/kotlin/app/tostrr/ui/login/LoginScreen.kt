package app.tostrr.ui.login

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import app.tostrr.R
import app.tostrr.ui.architecture.OnSideEffect
import app.tostrr.ui.architecture.watchAsState
import app.tostrr.ui.common.uiErrorText
import app.tostrr.ui.login.LoginViewModel.Action
import app.tostrr.ui.login.LoginViewModel.SideEffect
import app.tostrr.ui.login.components.LoginContent
import org.koin.androidx.compose.koinViewModel

/** Hands [redirect], the URL Tesla sent back through the deep link, to the login once; [onRedirectHandled] then clears it. */
@Composable
fun LoginScreen(
    redirect: String?,
    onRedirectHandled: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.watchAsState()
    val context = LocalContext.current

    LaunchedEffect(redirect) {
        if (redirect == null) return@LaunchedEffect
        viewModel.handle(Action.OnRedirect(redirect))
        onRedirectHandled()
    }
    OnSideEffect(viewModel) { effect ->
        when (effect) {
            is SideEffect.OpenLoginPage -> CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(effect.url))
        }
    }

    LoginContent(
        busy = state.busy,
        error = state.error?.let { uiErrorText(it, R.string.error_login_failed) },
        onLogin = { viewModel.handle(Action.OnLogin) },
        onPasteRedirect = { viewModel.handle(Action.OnRedirect(it)) },
    )
}
