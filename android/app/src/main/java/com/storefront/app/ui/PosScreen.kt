package com.storefront.app.ui

import android.widget.Toast
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
import com.storefront.app.network.NetworkModule
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@Composable
fun PosScreen(configManager: ConfigManager, viewModel: CartViewModel) {
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = configManager.authToken ?: return@launch
                val api = NetworkModule.createApiService(baseUrl)
                products = api.getProducts("Bearer $token")
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Product List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                ListItem(
                    headlineContent = { Text(product["name"].toString()) },
                    supportingContent = { Text("$${product["basePrice"] ?: product["price"]}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.addToCart(product) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
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
                            onSuccess = { Toast.makeText(context, "Order Placed!", Toast.LENGTH_SHORT).show() },
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
}
