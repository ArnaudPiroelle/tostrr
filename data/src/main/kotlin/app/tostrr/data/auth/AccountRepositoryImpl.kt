package app.tostrr.data.auth

import android.content.Context
import app.tostrr.data.api.TeslaApi
import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.TeslaAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File

/**
 * Reads the profile from the owner API `users/me` endpoint. The name and picture URL are kept encrypted in
 * [TokenStore], so logging out wipes them with the tokens; the picture itself is copied to the app's private,
 * never backed-up storage.
 */
@Single
class AccountRepositoryImpl(
    context: Context,
    private val api: TeslaApi,
    private val auth: TeslaAuth,
    private val store: TokenStore,
    private val client: OkHttpClient,
) : AccountRepository {
    private val avatarFile = File(context.noBackupFilesDir, "account_avatar")

    private val _account = MutableStateFlow(store.loadProfile()?.let { account(it.fullName) })
    override val account: StateFlow<TeslaAccount?> = _account.asStateFlow()

    override suspend fun load() {
        try {
            val token = auth.validAccessToken() ?: return
            val me = api.me(token) ?: return
            // Only the name and picture are kept: the email is never shown.
            val name = me.text("full_name") ?: return
            val avatarUrl = me.text("profile_image_url")
            val saved = store.loadProfile()
            when {
                avatarUrl == null -> avatarFile.delete()
                avatarUrl != saved?.avatarUrl || !avatarFile.exists() -> download(avatarUrl)
            }
            store.saveProfile(StoredProfile(name, avatarUrl))
            _account.value = account(name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only shown in the settings: the saved profile stays, the sync reports session problems.
        }
    }

    override fun clear() {
        _account.value = null
        avatarFile.delete()
    }

    private fun account(name: String) = TeslaAccount(name, avatarFile.takeIf { it.exists() }?.absolutePath)

    /** Written aside then moved, so a failed download never leaves half a picture. */
    private suspend fun download(url: String) = withContext(Dispatchers.IO) {
        val part = File(avatarFile.path + ".part")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@withContext
            part.outputStream().use { out -> response.body?.byteStream()?.copyTo(out) }
        }
        part.renameTo(avatarFile)
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}
