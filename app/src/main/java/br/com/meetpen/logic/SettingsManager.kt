package br.com.meetpen.logic

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("meetpen_prefs", Context.MODE_PRIVATE)

    fun saveOfflineModel(modelId: String) {
        prefs.edit().putString("offline_model_id", modelId).apply()
    }

    fun getOfflineModelId(): String {
        return prefs.getString("offline_model_id", "vosk_small") ?: "vosk_small"
    }

    fun saveApiKey(key: String, provider: String = "openai") {
        prefs.edit().putString("api_key_$provider", key).apply()
    }

    fun getApiKey(provider: String = "openai"): String? {
        return prefs.getString("api_key_$provider", null)
    }

    fun saveModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
    }

    fun getModel(): String {
        return prefs.getString("selected_model", "gpt-4o-mini") ?: "gpt-4o-mini"
    }

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun isSubscribed(): Boolean {
        return prefs.getBoolean("is_subscribed", false)
    }

    fun setSubscribed(subscribed: Boolean) {
        prefs.edit().putBoolean("is_subscribed", subscribed).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun setUserEmail(email: String?) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getUsageLimit(): Int {
        if (isSubscribed()) return Int.MAX_VALUE
        val email = getUserEmail()
        return if (email != null) {
            if (email == "fragosowallace@gmail.com") 20 else 5
        } else {
            5
        }
    }
}
