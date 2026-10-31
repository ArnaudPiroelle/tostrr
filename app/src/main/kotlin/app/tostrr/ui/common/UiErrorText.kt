package app.tostrr.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tostrr.R
import app.tostrr.domain.error.AuthError

/** [error] in words; an unexpected one is introduced by [unexpected], a format taking its detail. */
@Composable
internal fun uiErrorText(error: UiError, @StringRes unexpected: Int): String = when (error) {
    is UiError.Auth -> when (error.reason) {
        AuthError.NOT_LOGGED_IN -> stringResource(R.string.error_auth_not_logged_in)
        AuthError.NO_CODE -> stringResource(R.string.error_auth_no_code)
        AuthError.LOGIN_EXPIRED -> stringResource(R.string.error_auth_login_expired)
        AuthError.INVALID_STATE -> stringResource(R.string.error_auth_invalid_state)
        AuthError.HTTP -> stringResource(R.string.error_auth_http, error.httpCode ?: 0)
        AuthError.MISSING_TOKEN -> stringResource(R.string.error_auth_missing_token)
        AuthError.SESSION_EXPIRED -> stringResource(R.string.error_auth_session_expired)
    }
    is UiError.Api -> stringResource(R.string.error_api, error.code)
    is UiError.Unexpected -> stringResource(unexpected, error.detail.orEmpty())
}
