package app.tostrr.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.koin.core.annotation.Single
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The account's name and picture URL, kept so the settings show them at once, offline too. */
data class StoredProfile(val fullName: String, val avatarUrl: String?)

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
)

/** Stores OAuth tokens, the pending PKCE verifier and the account profile, encrypted with an Android Keystore AES key. */
@Single
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("tostrr_secure", Context.MODE_PRIVATE)

    fun load(): Tokens? {
        val access = read(KEY_ACCESS) ?: return null
        val refresh = read(KEY_REFRESH) ?: return null
        val exp = read(KEY_EXP)?.toLongOrNull() ?: 0L
        return Tokens(access, refresh, exp)
    }

    /** One commit for the three values, so a crash can never pair a new access token with an old refresh token. */
    fun save(tokens: Tokens) = write(
        KEY_ACCESS to tokens.accessToken,
        KEY_REFRESH to tokens.refreshToken,
        KEY_EXP to tokens.expiresAtEpochSec.toString(),
    )

    fun savePending(verifier: String, state: String, startedAtEpochMs: Long = System.currentTimeMillis()) =
        write(KEY_VERIFIER to verifier, KEY_STATE to state, KEY_STARTED to startedAtEpochMs.toString())

    fun pendingVerifier(): String? = read(KEY_VERIFIER)
    fun pendingState(): String? = read(KEY_STATE)

    /** When the pending login began; null for one saved by an older version, which kept no date. */
    fun pendingStartedAt(): Long? = read(KEY_STARTED)?.toLongOrNull()

    fun clearPending() = prefs.edit().remove(KEY_VERIFIER).remove(KEY_STATE).remove(KEY_STARTED).apply()

    fun saveProfile(profile: StoredProfile) =
        write(KEY_NAME to profile.fullName, KEY_AVATAR_URL to profile.avatarUrl.orEmpty())

    fun loadProfile(): StoredProfile? {
        val name = read(KEY_NAME) ?: return null
        return StoredProfile(name, read(KEY_AVATAR_URL)?.takeIf { it.isNotEmpty() })
    }

    /** Everything: tokens, pending login and profile. */
    fun clear() = prefs.edit().clear().apply()

    private fun write(vararg entries: Pair<String, String>) {
        val editor = prefs.edit()
        entries.forEach { (key, value) -> editor.putString(key, encrypt(key, value)) }
        editor.apply()
    }

    /** A value saved by an older version, encrypted without its key as associated data, is read once and saved again bound to it. */
    private fun read(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching {
            if (stored.startsWith(BOUND)) {
                decrypt(stored.removePrefix(BOUND), key)
            } else {
                decrypt(stored, aad = null).also { write(key to it) }
            }
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    /** The preference's [key] is the associated data: a value moved under another key no longer decrypts. */
    private fun encrypt(key: String, plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        cipher.updateAAD(key.toByteArray())
        val out = cipher.iv + cipher.doFinal(plain.toByteArray())
        return BOUND + Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String, aad: String?): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORM)
            .apply { init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv)) }
        aad?.let { cipher.updateAAD(it.toByteArray()) }
        return String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE))
    }

    private companion object {
        const val KEY_ALIAS = "tostrr_token_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        /** Marks a value encrypted with its key as associated data. */
        const val BOUND = "v2:"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_EXP = "exp"
        const val KEY_VERIFIER = "pkce_verifier"
        const val KEY_STATE = "pkce_state"
        const val KEY_STARTED = "pkce_started_at"
        const val KEY_NAME = "profile_name"
        const val KEY_AVATAR_URL = "profile_avatar_url"
    }
}
