package com.storefront.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.storefront.app.ui.DashboardActivity
import com.storefront.app.ui.LoginActivity
import com.storefront.app.ui.StoreSelectionActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configManager = ConfigManager(this)

        if (configManager.authToken == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else if (configManager.selectedStoreId == -1L) {
            startActivity(Intent(this, StoreSelectionActivity::class.java))
        } else {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        finish()
    }
}
