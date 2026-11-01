package app.tostrr.ui.common

import app.tostrr.domain.error.ApiException
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException

/** A failure a screen explains to the user, worded by [uiErrorText] in their language. */
sealed interface UiError {
    data class Auth(val reason: AuthError, val httpCode: Int? = null) : UiError

    data class Api(val code: Int) : UiError

    /** Anything else: the screen says what it was doing, followed by [detail]. */
    data class Unexpected(val detail: String?) : UiError
}

fun Throwable.toUiError(): UiError = when (this) {
    is AuthException -> UiError.Auth(reason, httpCode)
    is ApiException -> UiError.Api(code)
    else -> UiError.Unexpected(message)
}
