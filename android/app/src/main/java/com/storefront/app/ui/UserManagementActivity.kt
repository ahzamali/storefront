package com.storefront.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.storefront.app.ConfigManager

class UserManagementActivity : ComponentActivity() {
    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager(this)

        setContent {
            MaterialTheme {
                UserManagementScreen(configManager) { finish() }
            }
        }
    }
}
