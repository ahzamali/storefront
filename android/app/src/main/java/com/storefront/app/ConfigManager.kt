package com.storefront.app

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("storefront_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_BASE_URL = "https://storefront.softchef.in"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_SERVER_HISTORY = "server_history"
        private const val KEY_SELECTED_STORE_ID = "selected_store_id"
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null) ?: DEFAULT_BASE_URL
        set(value) {
            val cleanUrl = value.trim().removeSuffix("/")
            prefs.edit().putString(KEY_BASE_URL, cleanUrl).apply()
            addServerToHistory(cleanUrl)
        }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var userRole: String?
        get() = prefs.getString(KEY_USER_ROLE, null)
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var userId: Long?
        get() = prefs.getLong(KEY_USER_ID, -1L).takeIf { it != -1L }
        set(value) {
            if (value != null) prefs.edit().putLong(KEY_USER_ID, value).apply()
            else prefs.edit().remove(KEY_USER_ID).apply()
        }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

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
        get() {
            val saved = prefs.getStringSet(KEY_SERVER_HISTORY, emptySet()) ?: emptySet()
            return if (saved.isEmpty()) setOf(DEFAULT_BASE_URL) else saved
        }

    private fun addServerToHistory(url: String) {
        val currentHistory = serverHistory.toMutableSet()
        currentHistory.add(url)
        prefs.edit().putStringSet(KEY_SERVER_HISTORY, currentHistory).apply()
    }

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrBlank()
    
    fun clearAuth() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_SELECTED_STORE_ID)
            .apply()
    }
}
