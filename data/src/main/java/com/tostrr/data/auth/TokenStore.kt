package com.tostrr.data.auth

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

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
)

/** Stores OAuth tokens and the pending PKCE verifier, encrypted with an Android Keystore AES key. */
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

    fun savePending(verifier: String, state: String) = write(KEY_VERIFIER to verifier, KEY_STATE to state)

    fun pendingVerifier(): String? = read(KEY_VERIFIER)
    fun pendingState(): String? = read(KEY_STATE)

    fun clearPending() = prefs.edit().remove(KEY_VERIFIER).remove(KEY_STATE).apply()

    fun clear() = prefs.edit().clear().apply()

    private fun write(vararg entries: Pair<String, String>) {
        val editor = prefs.edit()
        entries.forEach { (key, value) -> editor.putString(key, encrypt(value)) }
        editor.apply()
    }

    private fun read(key: String): String? =
        prefs.getString(key, null)?.let { runCatching { decrypt(it) }.getOrNull() }

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

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val out = cipher.iv + cipher.doFinal(plain.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORM)
            .apply { init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv)) }
        return String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE))
    }

    private companion object {
        const val KEY_ALIAS = "tostrr_token_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_EXP = "exp"
        const val KEY_VERIFIER = "pkce_verifier"
        const val KEY_STATE = "pkce_state"
    }
}
