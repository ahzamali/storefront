package com.storefront.app.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.storefront.app.ConfigManager
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(configManager: ConfigManager) {
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = configManager.authToken ?: return@launch
                val api = NetworkModule.createApiService(baseUrl)
                products = api.getProducts("Bearer $token")
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    LazyColumn {
        items(products) { product ->
            ListItem(
                headlineContent = { Text(product["name"].toString()) },
                supportingContent = { Text("SKU: ${product["sku"]} - $${product["basePrice"]}") }
            )
            Divider()
        }
    }
}
