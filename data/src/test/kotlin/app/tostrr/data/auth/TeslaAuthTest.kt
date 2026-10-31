package app.tostrr.data.auth

import android.net.Uri
import android.util.Base64
import app.tostrr.data.testing.FakeAndroidKeyStore
import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.error.AuthError
import app.tostrr.domain.error.AuthException
import app.tostrr.domain.error.SessionExpiredException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class TeslaAuthTest {
    private val http = FakeHttp()
    private lateinit var store: TokenStore
    private lateinit var auth: TeslaAuth

    private val nowSec get() = System.currentTimeMillis() / 1000

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        store = TokenStore(RuntimeEnvironment.getApplication())
        auth = TeslaAuth(http.client, store)
    }

    private fun tokenAnswer(body: String, code: Int = 200) = http.on("/oauth2/v3/token", body, code)

    private suspend fun error(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()

    /** Starts a login and returns the redirect Tesla would send back for it. */
    private fun redirect(code: String? = "the-code", state: String? = null): String {
        val login = Uri.parse(auth.startLogin())
        return Uri.parse(TeslaAuth.REDIRECT_URI).buildUpon().apply {
            code?.let { appendQueryParameter("code", it) }
            appendQueryParameter("state", state ?: login.getQueryParameter("state"))
        }.build().toString()
    }

    @Test fun `the login page asks for a code with a pkce challenge of the saved verifier`() {
        val url = Uri.parse(auth.startLogin())
        assertEquals("auth.tesla.com", url.host)
        assertEquals("ownerapi", url.getQueryParameter("client_id"))
        assertEquals("tesla://auth/callback", url.getQueryParameter("redirect_uri"))
        assertEquals("code", url.getQueryParameter("response_type"))
        assertEquals("S256", url.getQueryParameter("code_challenge_method"))
        assertEquals(store.pendingState(), url.getQueryParameter("state"))
        val verifier = store.pendingVerifier()!!
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
        assertEquals(challenge, url.getQueryParameter("code_challenge"))
    }

    @Test fun `each login gets its own verifier and state`() {
        val first = Uri.parse(auth.startLogin()).getQueryParameter("state")
        val second = Uri.parse(auth.startLogin()).getQueryParameter("state")
        assertFalse(first == second)
    }

    @Test fun `completing the login exchanges the code and opens the session`() = runTest {
        tokenAnswer("""{"access_token":"A1","refresh_token":"R1","expires_in":3600}""")
        val redirect = redirect()
        val verifier = store.pendingVerifier()
        auth.completeLogin(redirect)

        val body = http.callsTo("/oauth2/v3/token").single().body!!
        assertTrue("grant_type=authorization_code" in body)
        assertTrue("code=the-code" in body)
        assertTrue("code_verifier=$verifier" in body)
        assertTrue(auth.isLoggedIn())
        assertEquals("A1", store.load()?.accessToken)
        assertEquals("R1", store.load()?.refreshToken)
        assertTrue(store.load()!!.expiresAtEpochSec in nowSec + 3590..nowSec + 3610)
        // The code can't be exchanged twice.
        assertNull(store.pendingVerifier())
    }

    @Test fun `a redirect without code is refused`() = runTest {
        val e = error { auth.completeLogin(redirect(code = null)) } as AuthException
        assertEquals(AuthError.NO_CODE, e.reason)
    }

    @Test fun `a redirect without a pending login is refused`() = runTest {
        val e = error { auth.completeLogin("tesla://auth/callback?code=c&state=s") } as AuthException
        assertEquals(AuthError.LOGIN_EXPIRED, e.reason)
    }

    @Test fun `a redirect with another state is refused before any request`() = runTest {
        val e = error { auth.completeLogin(redirect(state = "someone-else")) } as AuthException
        assertEquals(AuthError.INVALID_STATE, e.reason)
        assertTrue(http.calls.isEmpty())
        assertFalse(auth.isLoggedIn())
    }

    @Test fun `a token endpoint error keeps its http code`() = runTest {
        tokenAnswer("""{"error":"boom"}""", code = 500)
        val e = error { auth.completeLogin(redirect()) } as AuthException
        assertEquals(AuthError.HTTP, e.reason)
        assertEquals(500, e.httpCode)
        assertFalse(auth.isLoggedIn())
    }

    @Test fun `an answer without tokens is refused`() = runTest {
        tokenAnswer("""{"refresh_token":"R1"}""")
        assertEquals(AuthError.MISSING_TOKEN, (error { auth.completeLogin(redirect()) } as AuthException).reason)
        tokenAnswer("""{"access_token":"A1"}""")
        assertEquals(AuthError.MISSING_TOKEN, (error { auth.completeLogin(redirect()) } as AuthException).reason)
    }

    @Test fun `no session means no token`() = runTest {
        assertNull(auth.validAccessToken())
        assertTrue(http.calls.isEmpty())
    }

    @Test fun `a token still valid is used without a request`() = runTest {
        store.save(Tokens("A1", "R1", nowSec + 3600))
        assertEquals("A1", auth.validAccessToken())
        assertTrue(http.calls.isEmpty())
    }

    @Test fun `an expiring token is refreshed, the refresh token kept when none comes back`() = runTest {
        store.save(Tokens("A1", "R1", nowSec + 30))
        tokenAnswer("""{"access_token":"A2"}""")
        assertEquals("A2", auth.validAccessToken())
        val body = http.callsTo("/oauth2/v3/token").single().body!!
        assertTrue("grant_type=refresh_token" in body)
        assertTrue("refresh_token=R1" in body)
        assertEquals("R1", store.load()?.refreshToken)
        // Without expires_in, Tesla's usual eight hours.
        assertTrue(store.load()!!.expiresAtEpochSec in nowSec + 28_790..nowSec + 28_810)
    }

    @Test fun `a rejected refresh token ends the session`() = runTest {
        store.save(Tokens("A1", "R1", 0))
        tokenAnswer("{}", code = 401)
        assertTrue(error { auth.validAccessToken() } is SessionExpiredException)
        tokenAnswer("{}", code = 400)
        assertTrue(error { auth.validAccessToken() } is SessionExpiredException)
    }

    @Test fun `a server error on refresh keeps the session`() = runTest {
        store.save(Tokens("A1", "R1", 0))
        tokenAnswer("{}", code = 403)
        val e = error { auth.validAccessToken() }
        assertFalse(e is SessionExpiredException)
        assertEquals(403, (e as AuthException).httpCode)
        assertEquals("R1", store.load()?.refreshToken)
    }

    @Test fun `logging out forgets the tokens`() {
        store.save(Tokens("A1", "R1", nowSec + 3600))
        val auth = TeslaAuth(http.client, store)
        assertTrue(auth.isLoggedIn())
        auth.logout()
        assertFalse(auth.isLoggedIn())
        assertNull(store.load())
    }
}
