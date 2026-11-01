package app.tostrr.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExceptionsTest {
    @Test fun `an auth error names its reason, and the http code when there is one`() {
        assertEquals("NO_CODE", AuthException(AuthError.NO_CODE).message)
        assertEquals("HTTP (HTTP 503)", AuthException(AuthError.HTTP, 503).message)
    }

    @Test fun `an expired session is an auth error the sync can tell apart`() {
        val e: AuthException = SessionExpiredException()
        assertTrue(e is SessionExpiredException)
        assertEquals(AuthError.SESSION_EXPIRED, e.reason)
        assertNull(e.httpCode)
    }

    @Test fun `an api error keeps its http code`() {
        val e = ApiException(429)
        assertEquals(429, e.code)
        assertEquals("Tesla API HTTP 429", e.message)
    }
}
