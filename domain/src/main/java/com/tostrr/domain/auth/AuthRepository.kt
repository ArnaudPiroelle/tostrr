package com.tostrr.domain.auth

/** The Tesla account session. */
interface AuthRepository {
    /** Prepares a login and returns the URL of Tesla's login page. */
    fun startLogin(): String

    /** Finishes the login with the redirect URL Tesla sent back. Throws [com.tostrr.domain.error.AuthException]. */
    suspend fun completeLogin(redirect: String)

    fun isLoggedIn(): Boolean

    fun logout()
}
