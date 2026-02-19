package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.storefront.app.ConfigManager
import com.storefront.app.model.ProductStockDTO
import com.storefront.app.network.NetworkModule
import com.storefront.app.ui.components.CompactProductTable
import com.storefront.app.ui.components.ProductDetailDialog
import com.storefront.app.ui.pos.CartSection
import com.storefront.app.ui.pos.CheckoutScreen
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch

@Composable
fun PosScreen(configManager: ConfigManager, viewModel: CartViewModel) {
    var products by remember { mutableStateOf<List<ProductStockDTO>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductStockDTO?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isCheckoutMode by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = configManager.authToken ?: return@launch
                val api = NetworkModule.createApiService(baseUrl)
                val inventory = api.getInventoryView("Bearer $token")
                val bundles = api.getBundles("Bearer $token")
                products = inventory + bundles
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (isCheckoutMode) {
        CheckoutScreen(
            viewModel = viewModel,
            onConfirm = {
                viewModel.checkout(configManager,
                    onSuccess = {
                        Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                        isCheckoutMode = false
                    },
                    onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                )
            },
            onBack = { isCheckoutMode = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.45f).background(MaterialTheme.colorScheme.surface)) {
                CartSection(viewModel = viewModel, onCheckoutClick = { isCheckoutMode = true })
            }

            Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Inventory") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val filteredProducts = products.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true)
                }

                if (filteredProducts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No products found", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    CompactProductTable(
                        products = filteredProducts,
                        onProductClick = { selectedProduct = it },
                        onAddToCart = { viewModel.addToCart(it) }
                    )
                }
            }
        }
    }

    selectedProduct?.let {
        ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
    }
}
