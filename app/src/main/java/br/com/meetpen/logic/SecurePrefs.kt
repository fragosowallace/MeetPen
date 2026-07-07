package br.com.meetpen.logic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * SharedPreferences criptografado (Android Keystore) para dados sensíveis,
 * como as chaves de API dos provedores de IA.
 *
 * Na primeira utilização, migra automaticamente as chaves que estavam salvas
 * em texto puro no "meetpen_prefs" e as remove de lá.
 */
object SecurePrefs {
    private const val FILE_NAME = "meetpen_secure_prefs"
    private const val LEGACY_FILE_NAME = "meetpen_prefs"
    private val SENSITIVE_KEYS = listOf("api_key", "openai_key", "claude_key")

    @Volatile
    private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }
    }

    private fun create(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val secure = EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        migrateFromPlaintext(context, secure)
        return secure
    }

    private fun migrateFromPlaintext(context: Context, secure: SharedPreferences) {
        val legacy = context.getSharedPreferences(LEGACY_FILE_NAME, Context.MODE_PRIVATE)
        val secureEditor = secure.edit()
        val legacyEditor = legacy.edit()
        var migrated = false
        for (name in SENSITIVE_KEYS) {
            val value = legacy.getString(name, null)
            if (!value.isNullOrEmpty()) {
                if (secure.getString(name, null).isNullOrEmpty()) {
                    secureEditor.putString(name, value)
                }
                legacyEditor.remove(name)
                migrated = true
            }
        }
        if (migrated) {
            secureEditor.apply()
            legacyEditor.apply()
        }
    }
}
