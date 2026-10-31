package app.tostrr.domain.error

/** Why logging in failed or the session was lost; the screens word it. */
enum class AuthError {
    /** No session: never logged in, or logged out meanwhile. */
    NOT_LOGGED_IN,

    /** The redirect URL carries no authorisation code. */
    NO_CODE,

    /** The login was started too long ago, or by another install: no pending verifier. */
    LOGIN_EXPIRED,

    /** The redirect's OAuth state isn't the one this login sent. */
    INVALID_STATE,

    /** Tesla's token endpoint answered with an error, see [AuthException.httpCode]. */
    HTTP,

    /** Tesla answered without the access or refresh token. */
    MISSING_TOKEN,

    /** The refresh token was rejected: a new login is needed. */
    SESSION_EXPIRED,
}

/** Login or session problem. */
open class AuthException(val reason: AuthError, val httpCode: Int? = null) :
    Exception(httpCode?.let { "$reason (HTTP $it)" } ?: reason.name)

/** The refresh token was rejected (revoked/expired): the only case where we must drop the session. */
class SessionExpiredException : AuthException(AuthError.SESSION_EXPIRED)

/** Tesla's API answered with an HTTP error [code]. */
class ApiException(val code: Int) : Exception("Tesla API HTTP $code")
