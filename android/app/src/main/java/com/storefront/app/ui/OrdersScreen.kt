package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.storefront.app.model.OrderDTO
import com.storefront.app.model.Store
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(configManager: ConfigManager) {
    var orders by remember { mutableStateOf<List<OrderDTO>>(emptyList()) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedOrder by remember { mutableStateOf<OrderDTO?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadOrders() {
        scope.launch {
            try {
                isLoading = true
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                orders = api.getOrders(token, storeId = selectedStore?.id)
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading orders: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                stores = api.getStores(token)
                loadOrders()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(selectedStore) {
        loadOrders()
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Orders", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("Total: ${orders.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { loadOrders() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { expanded = true },
                            label = {
                                Text(
                                    text = selectedStore?.name ?: "All Stores",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("All Stores") }, onClick = { selectedStore = null; expanded = false })
                            stores.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expanded = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Customer Name, Phone, or Order ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = orders.filter { o ->
                    val query = searchQuery.trim().lowercase()
                    if (query.isEmpty()) true
                    else {
                        val idMatch = o.id.toString().contains(query)
                        val nameMatch = o.customer?.name?.lowercase()?.contains(query) == true
                        val phoneMatch = o.customer?.phone?.contains(query) == true
                        idMatch || nameMatch || phoneMatch
                    }
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No orders found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered) { order ->
                            OrderCardItem(order = order, onClick = { selectedOrder = order })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        OrderDetailDialog(order = order, onDismiss = { selectedOrder = null })
    }
}

@Composable
fun OrderCardItem(order: OrderDTO, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Order #${order.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "₹${order.totalAmount}", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Customer: ${order.customer?.name ?: "Walk-in Customer"}", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.DarkGray
                )
                if (order.store != null) {
                    Text("Store: ${order.store.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            if (!order.createdAt.isNullOrBlank()) {
                Text("Date: ${order.createdAt}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun OrderDetailDialog(order: OrderDTO, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Order Details #${order.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                if (order.store != null) {
                    Text("Store: ${order.store.name} (${order.store.type})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (!order.createdAt.isNullOrBlank()) {
                    Text("Time: ${order.createdAt}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Items (${order.orderLines.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(order.orderLines) { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${line.quantity}x ${line.productName ?: line.productSku}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (line.isExclusion) {
                                    Text("[Exclusion Applied]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("₹${line.unitPrice * line.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("₹${order.totalAmount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                if (order.customer?.name != null || order.customer?.phone != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Customer: ${order.customer.name ?: "N/A"} • ${order.customer.phone ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
