package app.tostrr.data.auth

import android.net.Uri
import app.tostrr.domain.auth.AuthRepository
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.error.SessionExpiredException
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Tesla owner-API OAuth2 (authorization code + PKCE) using the public "ownerapi" client.
 * The redirect target `tesla://auth/callback` is caught via the intent filter in the manifest
 * (or pasted by the user as a fallback).
 */
@Single
class TeslaAuth(
    private val client: OkHttpClient,
    private val store: TokenStore,
) : AuthRepository {
    private val _loggedIn = MutableStateFlow(store.load() != null)
    override val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    /** Builds the login URL and persists the PKCE verifier/state for the callback. */
    override fun startLogin(): String {
        val verifier = randomUrlSafe(32)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            URL_SAFE,
        )
        val state = randomUrlSafe(16)
        store.savePending(verifier, state)
        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /** Completes the login from the redirect URI (`tesla://auth/callback?code=...&state=...`). */
    override suspend fun completeLogin(redirect: String) {
        val uri = Uri.parse(redirect.trim())
        val code = uri.getQueryParameter("code") ?: throw AuthException(AuthError.NO_CODE)
        val verifier = store.pendingVerifier() ?: throw AuthException(AuthError.LOGIN_EXPIRED)
        // The state is saved with the verifier, so a redirect without it, or with another one, isn't ours.
        if (uri.getQueryParameter("state") != store.pendingState()) {
            throw AuthException(AuthError.INVALID_STATE)
        }
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", verifier)
            .build()
        tokenRequest(body, previousRefresh = null).also {
            store.save(it)
            // A code can only be exchanged once: replaying the same redirect must start a new login.
            store.clearPending()
            _loggedIn.value = true
        }
    }

    /** Returns a valid access token, refreshing it when expired. Null when not logged in. */
    suspend fun validAccessToken(): String? {
        val tokens = store.load() ?: return null
        if (tokens.expiresAtEpochSec - EXPIRY_MARGIN_SEC > System.currentTimeMillis() / 1000) {
            return tokens.accessToken
        }
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", CLIENT_ID)
            .add("refresh_token", tokens.refreshToken)
            .build()
        return tokenRequest(body, previousRefresh = tokens.refreshToken).also(store::save).accessToken
    }

    override fun logout() {
        store.clear()
        _loggedIn.value = false
    }

    private suspend fun tokenRequest(body: FormBody, previousRefresh: String?): Tokens =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(TOKEN_URL).post(body).build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // 400/401 on a refresh = refresh token invalid. 403/5xx are transient or edge blocks: keep the session.
                    if (previousRefresh != null && (response.code == 400 || response.code == 401)) throw SessionExpiredException()
                    throw AuthException(AuthError.HTTP, response.code)
                }
                val json = Json.parseToJsonElement(text).jsonObject
                val access = json["access_token"]?.jsonPrimitive?.content
                    ?: throw AuthException(AuthError.MISSING_TOKEN)
                val refresh = json["refresh_token"]?.jsonPrimitive?.content ?: previousRefresh
                    ?: throw AuthException(AuthError.MISSING_TOKEN)
                val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull ?: 28_800L
                Tokens(access, refresh, System.currentTimeMillis() / 1000 + expiresIn)
            }
        }

    private fun randomUrlSafe(bytes: Int): String =
        Base64.encodeToString(ByteArray(bytes).also { SecureRandom().nextBytes(it) }, URL_SAFE)

    companion object {
        const val CLIENT_ID = "ownerapi"
        const val REDIRECT_URI = "tesla://auth/callback"
        const val AUTH_URL = "https://auth.tesla.com/oauth2/v3/authorize"
        const val TOKEN_URL = "https://auth.tesla.com/oauth2/v3/token"
        const val SCOPE = "openid email offline_access"
        private const val EXPIRY_MARGIN_SEC = 60
        private const val URL_SAFE = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    }
}
