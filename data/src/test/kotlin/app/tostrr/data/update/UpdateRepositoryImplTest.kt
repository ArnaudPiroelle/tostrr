package app.tostrr.data.update

import android.content.Context
import app.tostrr.data.testing.FakeHttp
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.UpdateDownload
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class UpdateRepositoryImplTest {
    private val context = RuntimeEnvironment.getApplication()
    private val http = FakeHttp()
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)
    private val updatesDir = File(context.cacheDir, "updates")
    private val apkPath = "/ArnaudPiroelle/tostrr/releases/download/v0.2.0/tostrr-0.2.0.apk"
    private val apkUrl = "https://github.com$apkPath"
    private val offered = AppUpdate("0.2.0", "# New", apkUrl, 9)

    @Before fun setUp() {
        installed("0.1.0")
    }

    private fun installed(version: String) {
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName).versionName = version
    }

    private fun repository() = UpdateRepositoryImpl(context, http.client)

    private fun latest(tag: String = "v0.2.0", assets: String = """[{"name":"tostrr-0.2.0.apk","size":9,"browser_download_url":"$apkUrl"}]""") =
        http.on(LATEST, """{"tag_name":"$tag","body":"# New","prerelease":false,"assets":$assets}""")

    @Test fun `a newer release is offered, and still known at the next start without asking`() = runTest {
        latest()
        val repository = repository()
        assertNull(repository.update.value)

        repository.check()

        assertEquals(offered, repository.update.value)
        assertEquals(offered, repository.updateToOffer.value)
        assertEquals(offered, repository().update.value)
        assertEquals(1, http.callsTo(LATEST).size)
    }

    @Test fun `the installed version or an older one is not offered`() = runTest {
        latest(tag = "v0.1.0")
        val repository = repository()
        repository.check()
        assertNull(repository.update.value)
    }

    @Test fun `GitHub is asked again only after six hours`() = runTest {
        latest()
        val repository = repository()
        repository.check()
        repository.check()
        assertEquals(1, http.callsTo(LATEST).size)

        prefs.edit().putLong("update_checked_at", System.currentTimeMillis() - 6 * 3_600_000).apply()
        repository.check()
        assertEquals(2, http.callsTo(LATEST).size)
    }

    @Test fun `a failed check keeps what was known and asks again next time`() = runTest {
        latest()
        val repository = repository()
        repository.check()
        prefs.edit().putLong("update_checked_at", 0).apply()

        http.failOn(LATEST)
        repository.check()
        assertEquals(offered, repository.update.value)

        http.on(LATEST, "", code = 403)
        repository.check()
        http.on(LATEST, "not json")
        repository.check()
        assertEquals(offered, repository.update.value)
        assertEquals(4, http.callsTo(LATEST).size)
    }

    @Test fun `a release without APK is not offered`() = runTest {
        latest(assets = """[{"name":"notes.txt","size":9,"browser_download_url":"$apkUrl"}]""")
        val repository = repository()
        repository.check()
        assertNull(repository.update.value)
    }

    @Test fun `a skipped version is no longer offered, a later one is`() = runTest {
        latest()
        val repository = repository()
        repository.check()

        repository.skip("0.2.0")
        assertNull(repository.updateToOffer.value)
        assertNull(repository().updateToOffer.value)
        assertEquals(offered, repository.update.value)
        assertEquals(offered, repository().update.value)

        latest(tag = "v0.3.0")
        prefs.edit().putLong("update_checked_at", 0).apply()
        repository.check()
        assertEquals("0.3.0", repository.updateToOffer.value?.version)
    }

    @Test fun `the APK is downloaded with its progress, then found again`() = runTest {
        http.on(apkPath, "apk bytes")
        val repository = repository()

        val steps = repository.download(offered).toList()

        val apk = File(updatesDir, "tostrr-0.2.0.apk")
        assertEquals(UpdateDownload.Downloading(1f), steps[steps.size - 2])
        assertEquals(UpdateDownload.Ready(apk.absolutePath), steps.last())
        assertEquals("apk bytes", apk.readText())

        assertEquals(listOf<UpdateDownload>(UpdateDownload.Ready(apk.absolutePath)), repository.download(offered).toList())
        assertEquals(1, http.callsTo(apkPath).size)
    }

    @Test fun `an incomplete download fails and leaves nothing`() = runTest {
        http.on(apkPath, "apk")
        val repository = repository()
        assertThrows(IOException::class.java) { runBlocking { repository.download(offered).last() } }
        assertTrue(updatesDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun `a refused download fails`() = runTest {
        http.on(apkPath, "", code = 404)
        val repository = repository()
        assertThrows(IOException::class.java) { runBlocking { repository.download(offered).last() } }
    }

    @Test fun `the APK of the offered version is kept, older ones and installed ones are deleted`() = runTest {
        latest()
        http.on(apkPath, "apk bytes")
        val repository = repository()
        repository.check()
        repository.download(offered).last()
        val old = File(updatesDir, "tostrr-0.1.5.apk").apply { writeText("old") }

        repository.check()
        assertFalse(old.exists())
        assertTrue(File(updatesDir, "tostrr-0.2.0.apk").exists())

        installed("0.2.0")
        repository().check()
        assertFalse(File(updatesDir, "tostrr-0.2.0.apk").exists())
    }

    private companion object {
        const val LATEST = "/repos/ArnaudPiroelle/tostrr/releases/latest"
    }
}
