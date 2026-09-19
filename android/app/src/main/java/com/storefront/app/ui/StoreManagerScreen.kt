package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.storefront.app.ConfigManager
import com.storefront.app.model.*
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagerScreen(configManager: ConfigManager) {
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var masterProducts by remember { mutableStateOf<List<ProductStockDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var showCreateStoreDialog by remember { mutableStateOf(false) }
    var allocateStoreTarget by remember { mutableStateOf<Store?>(null) }
    var returnStoreTarget by remember { mutableStateOf<Store?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                stores = api.getStores(token)
                masterProducts = api.getInventoryView(token, null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading stores: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateStoreDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Store", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Store Management", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("Virtual Stores & Transfers", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = { loadData() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(stores) { store ->
                        StoreCard(
                            store = store,
                            isMaster = store.type == "MASTER",
                            onAllocate = { allocateStoreTarget = store },
                            onReturn = { returnStoreTarget = store }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Create Store Dialog
    if (showCreateStoreDialog) {
        CreateStoreDialog(
            configManager = configManager,
            onDismiss = { showCreateStoreDialog = false },
            onStoreCreated = {
                showCreateStoreDialog = false
                loadData()
            }
        )
    }

    // Stock Allocation Dialog
    allocateStoreTarget?.let { store ->
        StockTransferDialog(
            targetStore = store,
            isAllocation = true,
            products = masterProducts,
            configManager = configManager,
            onDismiss = { allocateStoreTarget = null },
            onSuccess = {
                allocateStoreTarget = null
                loadData()
            }
        )
    }

    // Return Stock Dialog
    returnStoreTarget?.let { store ->
        StockTransferDialog(
            targetStore = store,
            isAllocation = false,
            products = masterProducts,
            configManager = configManager,
            onDismiss = { returnStoreTarget = null },
            onSuccess = {
                returnStoreTarget = null
                loadData()
            }
        )
    }
}

@Composable
fun StoreCard(
    store: Store,
    isMaster: Boolean,
    onAllocate: () -> Unit,
    onReturn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMaster) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = if (isMaster) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        store.type, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (!store.location.isNullOrBlank()) {
                Text("Location: ${store.location}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (!isMaster) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAllocate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allocate Stock")
                    }
                    OutlinedButton(
                        onClick = onReturn,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Return Stock")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStoreDialog(
    configManager: ConfigManager,
    onDismiss: () -> Unit,
    onStoreCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create Virtual Store", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Store Name (e.g. Pop-up Stall A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            scope.launch {
                                isSubmitting = true
                                try {
                                    val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                    api.createStore("Bearer ${configManager.authToken}", mapOf("name" to name))
                                    Toast.makeText(context, "Store created successfully!", Toast.LENGTH_SHORT).show()
                                    onStoreCreated()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && name.isNotBlank()
                    ) {
                        Text(if (isSubmitting) "Creating..." else "Create Store")
                    }
                }
            }
        }
    }
}

@Composable
fun StockTransferDialog(
    targetStore: Store,
    isAllocation: Boolean,
    products: List<ProductStockDTO>,
    configManager: ConfigManager,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedSku by remember(products) { mutableStateOf(products.firstOrNull()?.sku ?: "") }
    var quantity by remember(targetStore.id, isAllocation) { mutableStateOf("5") }
    var isSubmitting by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val title = if (isAllocation) "Allocate Stock to ${targetStore.name}" else "Return Stock from ${targetStore.name}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Product:", style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedProd = products.find { it.sku == selectedSku }
                        Text(selectedProd?.let { "${it.name} (${it.sku})" } ?: "Select Product")
                    }
                    DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (${prod.sku})") },
                                onClick = {
                                    selectedSku = prod.sku
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Transfer Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val activeSku = selectedSku.ifBlank { products.firstOrNull()?.sku ?: "" }
                            if (activeSku.isBlank()) {
                                Toast.makeText(context, "Please select a product", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val qty = quantity.toIntOrNull() ?: 5
                            if (qty <= 0) {
                                Toast.makeText(context, "Please enter a valid quantity greater than 0", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isSubmitting = true
                                try {
                                    val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                    val req = AllocationRequestDTO(listOf(AllocationItemDTO(activeSku, qty)))
                                    val token = "Bearer ${configManager.authToken}"
                                    
                                    if (isAllocation) {
                                        api.allocateStock(token, targetStore.id, req)
                                        Toast.makeText(context, "Stock allocated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        api.returnStock(token, targetStore.id, req)
                                        Toast.makeText(context, "Stock returned successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    onSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Transfer failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "Processing..." else if (isAllocation) "Allocate" else "Return")
                    }
                }
            }
        }
    }
}
