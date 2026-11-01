package app.tostrr.domain.auth

import kotlinx.coroutines.flow.StateFlow

/** The Tesla account session. */
interface AuthRepository {
    /** Prepares a login and returns the URL of Tesla's login page. */
    fun startLogin(): String

    /** Finishes the login with the redirect URL Tesla sent back. Throws [app.tostrr.domain.error.AuthException]. */
    suspend fun completeLogin(redirect: String)

    /** Whether a session is open; flips on login and logout. */
    val loggedIn: StateFlow<Boolean>

    fun isLoggedIn(): Boolean = loggedIn.value

    fun logout()
}
