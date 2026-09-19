package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.storefront.app.ConfigManager
import com.storefront.app.model.OrderDTO
import com.storefront.app.model.ProductStockDTO
import com.storefront.app.network.NetworkModule
import com.storefront.app.ui.components.CompactProductTable
import com.storefront.app.ui.components.ProductDetailDialog
import com.storefront.app.viewmodel.CartItem
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun PosScreen(configManager: ConfigManager, viewModel: CartViewModel) {
    var products by remember { mutableStateOf<List<ProductStockDTO>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductStockDTO?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isCheckoutMode by remember { mutableStateOf(false) }
    var completedReceiptOrder by remember { mutableStateOf<OrderDTO?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadProducts() {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = configManager.authToken ?: return@launch
                val storeId = configManager.selectedStoreId
                val api = NetworkModule.createApiService(baseUrl)
                val inventory = api.getInventoryView("Bearer $token", storeId)
                val bundles = api.getBundles("Bearer $token")
                products = inventory + bundles
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(configManager.selectedStoreId) {
        loadProducts()
    }

    if (isCheckoutMode) {
        CheckoutConfirmationScreen(
            viewModel = viewModel,
            onConfirm = {
                viewModel.checkout(configManager,
                    onSuccess = { order ->
                        Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                        isCheckoutMode = false
                        completedReceiptOrder = order
                        loadProducts() // Refresh stock levels
                    },
                    onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                )
            },
            onBack = { isCheckoutMode = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP HALF: CART ---
            Box(modifier = Modifier.weight(0.48f).background(MaterialTheme.colorScheme.surface)) {
                CartSection(
                    viewModel = viewModel, 
                    onCheckoutClick = { isCheckoutMode = true },
                    onClearCart = { viewModel.clearCart() }
                )
            }

            Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // --- BOTTOM HALF: INVENTORY ---
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Inventory by Name or SKU") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        IconButton(onClick = {
                            try {
                                val scanner = GmsBarcodeScanning.getClient(context)
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        barcode.rawValue?.let { scanned ->
                                            searchQuery = scanned
                                            Toast.makeText(context, "Scanned: $scanned", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Scanner error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

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
                        onAddToCart = { product ->
                            if (product.quantity > 0 || product.type == "BUNDLE") {
                                viewModel.addToCart(product)
                            } else {
                                Toast.makeText(context, "Item is out of stock!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    selectedProduct?.let {
        ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
    }

    // Receipt Modal after successful checkout
    completedReceiptOrder?.let { order ->
        OrderReceiptDialog(order = order, onDismiss = { completedReceiptOrder = null })
    }
}

@Composable
fun CartSection(viewModel: CartViewModel, onCheckoutClick: () -> Unit, onClearCart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Current Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (viewModel.cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        if (viewModel.cartItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Cart is empty. Tap items below to add.", color = Color.Gray)
            }
        } else {
            // Cart List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { viewModel.incrementQuantity(item) },
                        onDecrement = { viewModel.removeOne(item) },
                        onDelete = { viewModel.removeItem(item) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total & Checkout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("₹${viewModel.totalAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCheckoutClick,
                enabled = viewModel.cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Proceed to Checkout (${viewModel.cartItems.sumOf { it.quantity }})")
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(
                "₹${item.price} each ${if (item.isBundle) "• [BUNDLE]" else ""}", 
                style = MaterialTheme.typography.bodySmall, 
                color = if (item.isBundle) MaterialTheme.colorScheme.secondary else Color.Gray
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(
                "${item.quantity}", 
                modifier = Modifier.padding(horizontal = 6.dp),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
        
        Text(
            "₹${item.price.multiply(BigDecimal(item.quantity))}", 
            style = MaterialTheme.typography.bodyMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CheckoutConfirmationScreen(
    viewModel: CartViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(viewModel.customerName ?: "") }
    var phone by remember { mutableStateOf(viewModel.customerPhone ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Review & Confirm Order", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Order Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                viewModel.cartItems.forEach {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.quantity}x ${it.name}", maxLines = 1, modifier = Modifier.weight(1f))
                        Text("₹${it.price.multiply(BigDecimal(it.quantity))}")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Payable Amount", fontWeight = FontWeight.Bold)
                    Text("₹${viewModel.totalAmount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Customer Details (Optional)", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; viewModel.setCustomer(it, phone) },
            label = { Text("Customer Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; viewModel.setCustomer(name, it) },
            label = { Text("Customer Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back to Cart")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("Confirm & Pay")
            }
        }
    }
}

@Composable
fun OrderReceiptDialog(order: OrderDTO, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle, 
                    contentDescription = "Success", 
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Order Receipt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Order #${order.id}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                if (order.store != null) {
                    Text("Store: ${order.store.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Itemized list
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(order.orderLines) { line ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${line.quantity}x ${line.productName ?: line.productSku}", maxLines = 1, modifier = Modifier.weight(1f))
                            Text("₹${line.unitPrice * line.quantity}")
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("₹${order.totalAmount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                if (order.customer?.name != null || order.customer?.phone != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Customer: ${order.customer.name ?: "N/A"} (${order.customer.phone ?: ""})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Done / Next Customer")
                }
            }
        }
    }
}
