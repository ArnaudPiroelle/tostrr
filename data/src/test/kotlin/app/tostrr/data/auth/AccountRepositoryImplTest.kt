package app.tostrr.data.auth

import app.tostrr.data.api.TeslaApi
import app.tostrr.data.testing.FakeAndroidKeyStore
import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.auth.TeslaAccount
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AccountRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()
    private val avatar = File(context.noBackupFilesDir, "account_avatar")
    private lateinit var store: TokenStore

    @Before fun setUp() {
        FakeAndroidKeyStore.install()
        store = TokenStore(context)
        store.save(Tokens("A1", "R1", System.currentTimeMillis() / 1000 + 3600))
    }

    private fun repository() = AccountRepositoryImpl(context, TeslaApi(http.client), TeslaAuth(http.client, store), store, http.client)

    private fun me(body: String, code: Int = 200) = http.on("/api/1/users/me", body, code)

    @Test fun `the name and picture are fetched and kept for the next start`() = runTest {
        me("""{"response":{"full_name":" Jane Doe ","email":"jane@example.com","profile_image_url":"https://img.example/jane.png"}}""")
        http.on("/jane.png", "picture bytes")
        val repository = repository()
        repository.load()
        assertEquals(TeslaAccount("Jane Doe", avatar.absolutePath), repository.account.value)
        assertEquals("picture bytes", avatar.readText())
        assertEquals(StoredProfile("Jane Doe", "https://img.example/jane.png"), store.loadProfile())
        assertEquals(TeslaAccount("Jane Doe", avatar.absolutePath), repository().account.value)
    }

    @Test fun `the same picture is not downloaded again`() = runTest {
        me("""{"response":{"full_name":"Jane Doe","profile_image_url":"https://img.example/jane.png"}}""")
        http.on("/jane.png", "picture bytes")
        val repository = repository()
        repository.load()
        repository.load()
        assertEquals(1, http.callsTo("/jane.png").size)
    }

    @Test fun `a failed picture download leaves no picture`() = runTest {
        me("""{"response":{"full_name":"Jane Doe","profile_image_url":"https://img.example/jane.png"}}""")
        http.on("/jane.png", "", code = 404)
        val repository = repository()
        repository.load()
        assertEquals(TeslaAccount("Jane Doe", null), repository.account.value)
        assertFalse(avatar.exists())
    }

    @Test fun `a picture removed from the account is deleted`() = runTest {
        me("""{"response":{"full_name":"Jane Doe","profile_image_url":"https://img.example/jane.png"}}""")
        http.on("/jane.png", "picture bytes")
        val repository = repository()
        repository.load()
        me("""{"response":{"full_name":"Jane Doe"}}""")
        repository.load()
        assertEquals(TeslaAccount("Jane Doe", null), repository.account.value)
        assertFalse(avatar.exists())
    }

    @Test fun `a failed fetch keeps the saved profile`() = runTest {
        store.saveProfile(StoredProfile("Jane Doe", null))
        me("", code = 500)
        val repository = repository()
        repository.load()
        assertEquals(TeslaAccount("Jane Doe", null), repository.account.value)
    }

    @Test fun `a profile without name changes nothing`() = runTest {
        me("""{"response":{"full_name":"  "}}""")
        val repository = repository()
        repository.load()
        assertNull(repository.account.value)
        assertNull(store.loadProfile())
    }

    @Test fun `logged out, nothing is fetched`() = runTest {
        store.clear()
        repository().load()
        assertTrue(http.calls.isEmpty())
    }

    @Test fun `clearing forgets the account and its picture`() = runTest {
        me("""{"response":{"full_name":"Jane Doe","profile_image_url":"https://img.example/jane.png"}}""")
        http.on("/jane.png", "picture bytes")
        val repository = repository()
        repository.load()
        repository.clear()
        assertNull(repository.account.value)
        assertFalse(avatar.exists())
    }
}
