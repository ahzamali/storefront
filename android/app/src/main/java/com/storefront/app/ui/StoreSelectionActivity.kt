package com.storefront.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.storefront.app.ConfigManager
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

class StoreSelectionActivity : ComponentActivity() {
    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StoreSelectionScreen()
                }
            }
        }
    }

    @Composable
    fun StoreSelectionScreen() {
        var stores by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val baseUrl = configManager.baseUrl ?: return@launch
                    val token = configManager.authToken ?: return@launch
                    val api = NetworkModule.createApiService(baseUrl)
                    stores = api.getStores("Bearer $token")
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load stores: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select Your Store", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (stores.isEmpty()) {
                CircularProgressIndicator()
                Text("Loading Stores...", modifier = Modifier.padding(top = 8.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(stores) { store ->
                        val id = (store["id"] as? Number)?.toLong() ?: -1L
                        val name = store["name"] as? String ?: "Unknown"
                        val type = store["type"] as? String ?: "Unknown"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    if (id != -1L) {
                                        configManager.selectedStoreId = id
                                        startActivity(Intent(this@StoreSelectionActivity, DashboardActivity::class.java))
                                        finish()
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(name, style = MaterialTheme.typography.titleLarge)
                                Text(type, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
