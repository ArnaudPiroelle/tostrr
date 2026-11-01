package app.tostrr.data.auth

import android.content.Context
import app.tostrr.data.testing.FakeAndroidKeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TokenStoreTest {
    private val context = RuntimeEnvironment.getApplication()
    private lateinit var store: TokenStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        store = TokenStore(context)
    }

    @Test fun `nothing saved means no session`() {
        assertNull(store.load())
        assertNull(store.pendingVerifier())
        assertNull(store.loadProfile())
    }

    @Test fun `tokens are read back by a new store`() {
        store.save(Tokens("access", "refresh", 1_800_000_000L))
        assertEquals(Tokens("access", "refresh", 1_800_000_000L), TokenStore(context).load())
    }

    @Test fun `nothing is stored in clear text`() {
        store.save(Tokens("my-access-token", "my-refresh-token", 1L))
        store.saveProfile(StoredProfile("Jane Doe", "https://img/jane.png"))
        val stored = context.getSharedPreferences("tostrr_secure", Context.MODE_PRIVATE).all.values.joinToString()
        listOf("my-access-token", "my-refresh-token", "Jane Doe", "https://img/jane.png").forEach {
            assertFalse(it, it in stored)
        }
    }

    @Test fun `clearing the pending login keeps the session`() {
        store.save(Tokens("access", "refresh", 1L))
        store.savePending("verifier", "state")
        assertEquals("verifier", store.pendingVerifier())
        assertEquals("state", store.pendingState())
        store.clearPending()
        assertNull(store.pendingVerifier())
        assertNull(store.pendingState())
        assertEquals("access", store.load()?.accessToken)
    }

    @Test fun `a profile without picture is read back without one`() {
        store.saveProfile(StoredProfile("Jane Doe", null))
        assertEquals(StoredProfile("Jane Doe", null), store.loadProfile())
    }

    @Test fun `clearing forgets the tokens, the pending login and the profile`() {
        store.save(Tokens("access", "refresh", 1L))
        store.savePending("verifier", "state")
        store.saveProfile(StoredProfile("Jane Doe", "https://img/jane.png"))
        store.clear()
        assertNull(store.load())
        assertNull(store.pendingVerifier())
        assertNull(store.loadProfile())
    }

    @Test fun `a value that can no longer be decrypted reads as missing`() {
        context.getSharedPreferences("tostrr_secure", Context.MODE_PRIVATE).edit()
            .putString("access", "bm90IGVuY3J5cHRlZCBhdCBhbGw=").putString("refresh", "garbage").commit()
        assertNull(store.load())
    }
}
