package com.storefront.app

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("storefront_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SERVER_HISTORY = "server_history"
        private const val KEY_SELECTED_STORE_ID = "selected_store_id"
    }

    var baseUrl: String?
        get() = prefs.getString(KEY_BASE_URL, null)
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value).apply()
            if (value != null) {
                addServerToHistory(value)
            }
        }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var selectedStoreId: Long?
        get() = prefs.getLong(KEY_SELECTED_STORE_ID, -1L).takeIf { it != -1L }
        set(value) {
            if (value != null) {
                prefs.edit().putLong(KEY_SELECTED_STORE_ID, value).apply()
            } else {
                prefs.edit().remove(KEY_SELECTED_STORE_ID).apply()
            }
        }

    val serverHistory: Set<String>
        get() = prefs.getStringSet(KEY_SERVER_HISTORY, emptySet()) ?: emptySet()

    private fun addServerToHistory(url: String) {
        val currentHistory = serverHistory.toMutableSet()
        currentHistory.add(url)
        prefs.edit().putStringSet(KEY_SERVER_HISTORY, currentHistory).apply()
    }
    
    fun clearAuth() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_SELECTED_STORE_ID).apply()
    }
}
