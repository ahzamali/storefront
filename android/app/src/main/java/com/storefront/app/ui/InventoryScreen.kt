package com.storefront.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

    // Add Product Dialog State
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
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
    }

    selectedProduct?.let {
        ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
    }

    // Add Product by ISBN Dialog
    if (showAddDialog) {
        var isbn by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("1") }
        var price by remember { mutableStateOf("") }
        var isAdding by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add by ISBN") },
            text = {
                Column {
                    Text("Enter ISBN to fetch details automatically.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = isbn, 
                        onValueChange = { isbn = it }, 
                        label = { Text("ISBN / SKU") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { quantity = it }, 
                        label = { Text("Quantity") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price, 
                        onValueChange = { price = it }, 
                        label = { Text("Price (Required)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isAdding = true
                            try {
                                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                val payload = mapOf(
                                    "isbn" to isbn,
                                    "quantity" to (quantity.toIntOrNull() ?: 1),
                                    "price" to (price.toDoubleOrNull() ?: 0.0)
                                )
                                api.ingestIsbn("Bearer ${configManager.authToken}", payload)
                                showAddDialog = false
                                loadInventory(selectedStore?.id) // Refresh
                            } catch (e: Exception) {
                                e.printStackTrace() // In real app show error toast
                            } finally {
                                isAdding = false
                            }
                        }
                    },
                    enabled = !isAdding && isbn.isNotBlank() && price.isNotBlank()
                ) {
                    if (isAdding) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("Add Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProductItem(product: ProductStockDTO, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(product.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { 
            Column {
                Text("SKU: ${product.sku}")
                Text("Price: $${product.price}", color = MaterialTheme.colorScheme.primary)
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
