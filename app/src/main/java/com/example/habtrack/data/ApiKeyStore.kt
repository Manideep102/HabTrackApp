package com.example.habtrack.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's Anthropic API key on-device using EncryptedSharedPreferences,
 * so it never needs to be hardcoded or committed to source control.
 */
class ApiKeyStore(context: Context) {

    private val appContext = context.applicationContext

    // Nullable: the Tink keyset backing EncryptedSharedPreferences can become
    // unreadable (emulator snapshot restore, device backup/restore, keystore
    // drift), which throws on create(). Losing the stored key and letting the
    // user re-enter it beats crashing Settings forever, so we wipe the corrupt
    // prefs file and retry once; null means even that failed.
    private val prefs: SharedPreferences? by lazy {
        try {
            createPrefs()
        } catch (_: Throwable) {
            try {
                appContext.deleteSharedPreferences(PREFS_FILE)
                createPrefs()
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun createPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Individual values can still fail to decrypt even when the keyset loaded,
    // so reads/writes are guarded too; a lost key is always re-enterable.
    fun getApiKey(): String? = try {
        prefs?.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }

    fun saveApiKey(apiKey: String) {
        try {
            prefs?.edit()?.putString(KEY_API_KEY, apiKey.trim())?.apply()
        } catch (_: Throwable) {
        }
    }

    fun clearApiKey() {
        try {
            prefs?.edit()?.remove(KEY_API_KEY)?.apply()
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val PREFS_FILE = "habtrack_secure_prefs"
        private const val KEY_API_KEY = "anthropic_api_key"
    }
}
