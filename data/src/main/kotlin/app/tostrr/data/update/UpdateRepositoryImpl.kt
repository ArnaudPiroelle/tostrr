package app.tostrr.data.update

import android.content.Context
import android.content.pm.ApplicationInfo
import app.tostrr.domain.update.AppUpdate
import app.tostrr.domain.update.AppVersions
import app.tostrr.domain.update.UpdateDownload
import app.tostrr.domain.update.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reads the latest release from GitHub's public API, which leaves out drafts and pre-releases. It is kept in shared
 * preferences so it can be offered again without asking, and the APK goes to the cache, so it survives the app being
 * restarted while the user allows installs. A debug build is never offered anything: Android would refuse to install
 * a release, signed with another key, over it.
 */
@Single
class UpdateRepositoryImpl(context: Context, private val client: OkHttpClient) : UpdateRepository {
    private val prefs = context.getSharedPreferences("tostrr_settings", Context.MODE_PRIVATE)
    private val updatesDir = File(context.cacheDir, "updates")
    private val installedVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    private val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val _update = MutableStateFlow(newerRelease())
    override val update: StateFlow<AppUpdate?> = _update.asStateFlow()

    private val _updateToOffer = MutableStateFlow(newerRelease().unlessSkipped())
    override val updateToOffer: StateFlow<AppUpdate?> = _updateToOffer.asStateFlow()

    override suspend fun check() {
        if (debuggable) return
        withContext(Dispatchers.IO) {
            try {
                if (System.currentTimeMillis() - prefs.getLong(KEY_CHECKED_AT, 0) >= CHECK_INTERVAL_MS) fetchLatest()?.let(::save)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Only an offer: the next start asks again.
            }
            // The newer release's APK stays, whether downloaded or on its way (.part); nothing older is of any use.
            val apkToKeep = _update.value?.let(::apkFile)?.name
            updatesDir.listFiles()?.filter { apkToKeep == null || !it.name.startsWith(apkToKeep) }?.forEach { it.delete() }
        }
    }

    override fun skip(version: String) {
        prefs.edit().putString(KEY_SKIPPED, version).apply()
        publish()
    }

    override fun download(update: AppUpdate): Flow<UpdateDownload> = flow {
        val apk = apkFile(update)
        if (apk.length() != update.sizeBytes) fetchApk(update, apk)
        emit(UpdateDownload.Ready(apk.absolutePath))
    }.flowOn(Dispatchers.IO)

    /** Null when GitHub has nothing usable: an error, or a release without APK. */
    private fun fetchLatest(): AppUpdate? {
        val request = Request.Builder().url(LATEST_RELEASE_URL).header("Accept", "application/vnd.github+json").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            parseRelease(Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject)
        }
    }

    private fun parseRelease(release: JsonObject): AppUpdate? {
        val version = release.text("tag_name")?.removePrefix("v") ?: return null
        val apk = (release["assets"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.text("name")?.endsWith(".apk") == true } ?: return null
        return AppUpdate(
            version = version,
            notes = release.text("body").orEmpty(),
            downloadUrl = apk.text("browser_download_url") ?: return null,
            sizeBytes = (apk["size"] as? JsonPrimitive)?.longOrNull ?: return null,
        )
    }

    private fun save(release: AppUpdate) {
        prefs.edit()
            .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
            .putString(KEY_VERSION, release.version)
            .putString(KEY_NOTES, release.notes)
            .putString(KEY_URL, release.downloadUrl)
            .putLong(KEY_SIZE, release.sizeBytes)
            .apply()
        publish()
    }

    private fun publish() {
        val newer = newerRelease()
        _update.value = newer
        _updateToOffer.value = newer.unlessSkipped()
    }

    private fun AppUpdate?.unlessSkipped() = takeIf { this?.version != prefs.getString(KEY_SKIPPED, null) }

    private fun newerRelease(): AppUpdate? {
        if (debuggable) return null
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        if (!AppVersions.isNewer(version, installedVersion)) return null
        return AppUpdate(
            version = version,
            notes = prefs.getString(KEY_NOTES, null).orEmpty(),
            downloadUrl = prefs.getString(KEY_URL, null) ?: return null,
            sizeBytes = prefs.getLong(KEY_SIZE, 0),
        )
    }

    private fun apkFile(update: AppUpdate) = File(updatesDir, "tostrr-${update.version}.apk")

    /** Written aside then moved, so an interrupted download never passes for the APK. */
    private suspend fun FlowCollector<UpdateDownload>.fetchApk(update: AppUpdate, apk: File) {
        updatesDir.mkdirs()
        val part = File(apk.path + ".part")
        try {
            client.newCall(Request.Builder().url(update.downloadUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val input = response.body?.byteStream() ?: throw IOException("No body")
                part.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    var percent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        val newPercent = (written * 100 / update.sizeBytes.coerceAtLeast(1)).toInt()
                        if (newPercent != percent) {
                            percent = newPercent
                            emit(UpdateDownload.Downloading(written.toFloat() / update.sizeBytes.coerceAtLeast(1)))
                        }
                    }
                }
            }
            if (part.length() != update.sizeBytes) throw IOException("Incomplete download")
            if (!part.renameTo(apk)) throw IOException("Can't move the download")
        } finally {
            part.delete()
        }
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/ArnaudPiroelle/tostrr/releases/latest"
        val CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)

        const val KEY_CHECKED_AT = "update_checked_at"
        const val KEY_SKIPPED = "update_skipped_version"
        const val KEY_VERSION = "update_version"
        const val KEY_NOTES = "update_notes"
        const val KEY_URL = "update_url"
        const val KEY_SIZE = "update_size"
    }
}
