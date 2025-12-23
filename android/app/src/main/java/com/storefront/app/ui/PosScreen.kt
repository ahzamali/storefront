package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.storefront.app.ConfigManager
import com.storefront.app.model.ProductStockDTO
import com.storefront.app.network.NetworkModule
import com.storefront.app.ui.components.ProductDetailDialog
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@Composable
fun PosScreen(configManager: ConfigManager, viewModel: CartViewModel) {
    var products by remember { mutableStateOf<List<ProductStockDTO>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductStockDTO?>(null) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = configManager.authToken ?: return@launch
                val api = NetworkModule.createApiService(baseUrl)
                
                // Load global products and bundles
                // In real POS, this should be store-specific inventory, but Web uses global product definitions often 
                // However, Web POS usually filters by store inventory. 
                // Ideally, we should fetch 'inventory view' for the current store.
                // Assuming ConfigManager has current store logic or defaults.
                // For now, let's load global Inventory View for consistency with simple POS logic
                val inventory = api.getInventoryView("Bearer $token")
                val bundles = api.getBundles("Bearer $token")
                products = inventory + bundles
                
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Customer Details
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Customer Details", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerName, 
                        onValueChange = { customerName = it; viewModel.setCustomer(it, customerPhone) },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customerPhone, 
                        onValueChange = { customerPhone = it; viewModel.setCustomer(customerName, it) },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Divider()

        // Product List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                ListItem(
                    headlineContent = { 
                        Text(
                            product.name, 
                            modifier = Modifier.clickable { selectedProduct = product }
                        ) 
                    },
                    supportingContent = { Text("$${product.basePrice}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.addToCart(product) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    },
                    leadingContent = {
                        if (product.type == "BUNDLE") {
                            Text("📦", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                )
                Divider()
            }
        }

        // Cart Summary
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cart: ${viewModel.cartItems.sumOf { it.quantity }} items", style = MaterialTheme.typography.titleMedium)
                Text("Total: $${viewModel.totalAmount}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        viewModel.checkout(configManager,
                            onSuccess = { 
                                Toast.makeText(context, "Order Placed!", Toast.LENGTH_SHORT).show() 
                                customerName = ""
                                customerPhone = ""
                            },
                            onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.cartItems.isNotEmpty()
                ) {
                    Text("Checkout")
                }
            }
        }
    }
    
    selectedProduct?.let {
        ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
    }
}
