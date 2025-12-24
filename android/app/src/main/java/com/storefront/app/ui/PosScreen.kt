package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storefront.app.ConfigManager
import com.storefront.app.model.ProductStockDTO
import com.storefront.app.network.NetworkModule
import com.storefront.app.ui.components.CompactProductTable
import com.storefront.app.ui.components.ProductDetailDialog
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
        CheckoutConfirmationScreen(
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
            // --- TOP HALF: CART ---
            Box(modifier = Modifier.weight(0.45f).background(MaterialTheme.colorScheme.surface)) {
                CartSection(viewModel = viewModel, onCheckoutClick = { isCheckoutMode = true })
            }

            Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // --- BOTTOM HALF: INVENTORY ---
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                // Search Bar
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

                // Inventory List
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

@Composable
fun CartSection(viewModel: CartViewModel, onCheckoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Current Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))

        // Cart List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.cartItems) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(item.sku, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.removeOne(item) }, modifier = Modifier.size(24.dp)) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { /* Add functionality if needed */ }, modifier = Modifier.size(24.dp)) {
                            // Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    
                    Text(
                        "₹${item.price.multiply(BigDecimal(item.quantity))}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
                Divider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total & Checkout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: ₹${viewModel.totalAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = onCheckoutClick,
                enabled = viewModel.cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Checkout")
            }
        }
    }
}

@Composable
fun CheckoutConfirmationScreen(
    viewModel: CartViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Checkout Confirmation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Order Summary", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                viewModel.cartItems.forEach {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.quantity} x ${it.name}", maxLines = 1, modifier = Modifier.weight(1f))
                        Text("₹${it.price.multiply(BigDecimal(it.quantity))}")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold)
                    Text("₹${viewModel.totalAmount}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Customer Information (Optional)", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; viewModel.setCustomer(it, phone) },
            label = { Text("Customer Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; viewModel.setCustomer(name, it) },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("Confirm Order")
            }
        }
    }
}
