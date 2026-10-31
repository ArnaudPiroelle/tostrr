package app.tostrr.data.auth

import app.tostrr.data.api.TeslaApi
import app.tostrr.domain.auth.AccountRepository
import app.tostrr.domain.auth.TeslaAccount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import org.koin.core.annotation.Single

/** Reads the profile from the owner API `users/me` endpoint; held in memory only. */
@Single
class AccountRepositoryImpl(
    private val api: TeslaApi,
    private val auth: TeslaAuth,
) : AccountRepository {
    private val _account = MutableStateFlow<TeslaAccount?>(null)
    override val account: StateFlow<TeslaAccount?> = _account.asStateFlow()

    override suspend fun load() {
        try {
            val token = auth.validAccessToken() ?: return
            val me = api.me(token) ?: return
            // Only the name and picture are kept: the email is never shown.
            me.text("full_name")?.let { _account.value = TeslaAccount(it, avatarUrl = me.text("profile_image_url")) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only shown in the settings: missing is fine, the sync reports session problems.
        }
    }

    override fun clear() {
        _account.value = null
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}
