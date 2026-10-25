package com.tostrr.domain.error

/** Login or session problem; the message is meant for the user. */
open class AuthException(message: String) : Exception(message)

/** The refresh token was rejected (revoked/expired): the only case where we must drop the session. */
class SessionExpiredException : AuthException("Session expirée, reconnectez-vous.")

/** Tesla's API answered with an error; the message is meant for the user. */
class ApiException(val code: Int, message: String) : Exception(message)
