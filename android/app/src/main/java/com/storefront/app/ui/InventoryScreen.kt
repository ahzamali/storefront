package com.storefront.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.storefront.app.ConfigManager
import com.storefront.app.model.ProductStockDTO
import com.storefront.app.model.Store
import com.storefront.app.network.NetworkModule
import com.storefront.app.ui.components.ProductDetailDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(configManager: ConfigManager) {
    var products by remember { mutableStateOf<List<ProductStockDTO>>(emptyList()) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var selectedProduct by remember { mutableStateOf<ProductStockDTO?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    fun loadInventory(storeId: Long?) {
        scope.launch {
            try {
                isLoading = true
                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                val token = "Bearer ${configManager.authToken}"
                products = api.getInventoryView(token, storeId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
             try {
                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                val token = "Bearer ${configManager.authToken}"
                stores = api.getStores(token)
                loadInventory(null)
             } catch(e: Exception) {}
        }
    }

    LaunchedEffect(selectedStore) {
        loadInventory(selectedStore?.id)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Bar: Store Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inventory", style = MaterialTheme.typography.headlineSmall)
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text(selectedStore?.name ?: "HQ (Master)")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("HQ (Master)") }, onClick = { selectedStore = null; expanded = false })
                    stores.forEach { s ->
                        DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expanded = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val filtered = products.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.sku.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn {
                items(filtered) { product ->
                    ProductItem(product) { selectedProduct = product }
                    Divider()
                }
            }
        }
    }

    selectedProduct?.let {
        ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
    }
}

@Composable
fun ProductItem(product: ProductStockDTO, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(product.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { 
            Column {
                Text("SKU: ${product.sku}")
                Text("Price: $${product.basePrice}", color = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = {
            Text(
                text = "${product.quantity}",
                style = MaterialTheme.typography.titleLarge,
                color = if (product.quantity > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
