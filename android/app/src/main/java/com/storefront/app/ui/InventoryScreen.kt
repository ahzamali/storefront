package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.storefront.app.ConfigManager
import com.storefront.app.model.*
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
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Books, 2: Stationery, 3: Bundles
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var showRestockDialogFor by remember { mutableStateOf<ProductStockDTO?>(null) }
    var showEditDialogFor by remember { mutableStateOf<ProductStockDTO?>(null) }
    var showDeleteConfirmFor by remember { mutableStateOf<ProductStockDTO?>(null) }

    fun loadInventory(storeId: Long?) {
        scope.launch {
            try {
                isLoading = true
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                val inv = api.getInventoryView(token, storeId)
                val bundles = api.getBundles(token)
                products = inv + bundles
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading inventory: ${e.message}", Toast.LENGTH_SHORT).show()
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
                loadInventory(configManager.selectedStoreId)
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(selectedStore) {
        loadInventory(selectedStore?.id)
    }

    val tabs = listOf("All", "Books", "Stationery", "Bundles")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product / Bundle", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // Header with Store Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventory", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("Total Products: ${products.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = {
                            Text(
                                text = selectedStore?.name ?: "HQ (Master)",
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = null, 
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("HQ (Master Store)") }, onClick = { selectedStore = null; expanded = false })
                        stores.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name or SKU") },
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
                val filtered = products.filter { p ->
                    val matchesQuery = p.name.contains(searchQuery, ignoreCase = true) || 
                                       p.sku.contains(searchQuery, ignoreCase = true)
                    val matchesTab = when (selectedTab) {
                        1 -> p.type == "BOOK"
                        2 -> p.type == "STATIONERY" || p.type == "PENCIL"
                        3 -> p.type == "BUNDLE"
                        else -> true
                    }
                    matchesQuery && matchesTab
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No products in this category", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered) { product ->
                            ProductListItem(
                                product = product,
                                onClick = { selectedProduct = product }
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // Product Detail Dialog
    selectedProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onAddStock = {
                showRestockDialogFor = product
                selectedProduct = null
            },
            onEdit = {
                showEditDialogFor = product
                selectedProduct = null
            },
            onDelete = {
                showDeleteConfirmFor = product
                selectedProduct = null
            }
        )
    }

    // Add Stock / Restock Dialog
    showRestockDialogFor?.let { product ->
        var restockQuantity by remember { mutableStateOf("10") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRestockDialogFor = null },
            title = { Text("Add Stock - ${product.name}") },
            text = {
                Column {
                    Text("Current stock: ${product.quantity}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restockQuantity,
                        onValueChange = { restockQuantity = it },
                        label = { Text("Quantity to Add") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = restockQuantity.toIntOrNull() ?: 0
                        if (qty > 0) {
                            scope.launch {
                                isSaving = true
                                try {
                                    val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                    api.addStock("Bearer ${configManager.authToken}", AddStockRequest(product.sku, qty))
                                    Toast.makeText(context, "Stock added successfully!", Toast.LENGTH_SHORT).show()
                                    showRestockDialogFor = null
                                    loadInventory(selectedStore?.id)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Adding..." else "Add Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestockDialogFor = null }) { Text("Cancel") }
            }
        )
    }

    // Edit Product Dialog
    showEditDialogFor?.let { product ->
        var editName by remember { mutableStateOf(product.name) }
        var editPrice by remember { mutableStateOf(product.price.toString()) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialogFor = null },
            title = { Text("Edit Product Details") },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Product Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("Price (₹)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                val req = CreateProductRequest(
                                    sku = product.sku,
                                    name = editName,
                                    basePrice = editPrice.toDoubleOrNull() ?: product.price,
                                    type = product.type,
                                    attributes = product.attributes ?: ProductAttributes()
                                )
                                api.updateProduct("Bearer ${configManager.authToken}", product.id, req)
                                Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
                                showEditDialogFor = null
                                loadInventory(selectedStore?.id)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Saving..." else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogFor = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmFor?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmFor = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to permanently delete '${product.name}' (${product.sku})?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                api.deleteProduct("Bearer ${configManager.authToken}", product.id)
                                Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmFor = null
                                loadInventory(selectedStore?.id)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmFor = null }) { Text("Cancel") }
            }
        )
    }

    // Add Product / Bundle Modal
    if (showAddDialog) {
        AddProductOrBundleDialog(
            products = products,
            onDismiss = { showAddDialog = false },
            onProductAdded = {
                showAddDialog = false
                loadInventory(selectedStore?.id)
            },
            configManager = configManager
        )
    }
}

@Composable
fun ProductListItem(product: ProductStockDTO, onClick: () -> Unit) {
    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (product.type == "BUNDLE") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "BUNDLE", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        supportingContent = { 
            Column {
                Text("SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall)
                Text("₹${product.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.quantity}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (product.quantity > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Text(if (product.quantity > 0) "In Stock" else "Out of Stock", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun AddProductOrBundleDialog(
    products: List<ProductStockDTO>,
    onDismiss: () -> Unit,
    onProductAdded: () -> Unit,
    configManager: ConfigManager
) {
    var mode by remember { mutableStateOf("CHOICE") } // CHOICE, ISBN, MANUAL, BUNDLE
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }

    // Manual / ISBN fields
    var isbn by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var itemType by remember { mutableStateOf("BOOK") }
    var author by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }

    // Bundle fields
    var bundleSku by remember { mutableStateOf("") }
    var bundleName by remember { mutableStateOf("") }
    var bundlePrice by remember { mutableStateOf("") }
    val selectedProductSkus = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    when (mode) {
                        "CHOICE" -> "Add New Item"
                        "ISBN" -> "Scan / Ingest ISBN"
                        "MANUAL" -> "Add Product Manually"
                        "BUNDLE" -> "Create Product Bundle"
                        else -> "Add Item"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (mode) {
                    "CHOICE" -> {
                        Button(onClick = { mode = "ISBN" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Auto-Ingest via ISBN (Books)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { mode = "MANUAL" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Create Standard Product")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { mode = "BUNDLE" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Create Multi-Product Bundle")
                        }
                    }

                    "ISBN" -> {
                        FilledTonalButton(
                            onClick = {
                                try {
                                    val scanner = GmsBarcodeScanning.getClient(context)
                                    scanner.startScan()
                                        .addOnSuccessListener { barcode ->
                                            barcode.rawValue?.let { scanned ->
                                                isbn = scanned
                                                Toast.makeText(context, "Scanned: $scanned", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Scan cancelled/failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Scanner error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Book Barcode with Camera")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = isbn, 
                            onValueChange = { isbn = it }, 
                            label = { Text("ISBN (10 or 13 digits)") }, 
                            singleLine = true, 
                            trailingIcon = {
                                IconButton(onClick = {
                                    try {
                                        val scanner = GmsBarcodeScanning.getClient(context)
                                        scanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                barcode.rawValue?.let { scanned ->
                                                    isbn = scanned
                                                }
                                            }
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }

                    "MANUAL" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = itemType == "BOOK", onClick = { itemType = "BOOK" })
                            Text("Book")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = itemType == "STATIONERY", onClick = { itemType = "STATIONERY" })
                            Text("Stationery")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        if (itemType == "BOOK") {
                            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        } else {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    "BUNDLE" -> {
                        OutlinedTextField(value = bundleSku, onValueChange = { bundleSku = it }, label = { Text("Bundle SKU *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bundleName, onValueChange = { bundleName = it }, label = { Text("Bundle Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bundlePrice, onValueChange = { bundlePrice = it }, label = { Text("Package Price (₹) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select Included Products (${selectedProductSkus.size} selected):", style = MaterialTheme.typography.bodySmall)
                        
                        val standardProducts = products.filter { it.type != "BUNDLE" }
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(standardProducts) { prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (selectedProductSkus.contains(prod.sku)) {
                                            selectedProductSkus.remove(prod.sku)
                                        } else {
                                            selectedProductSkus.add(prod.sku)
                                        }
                                    }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedProductSkus.contains(prod.sku),
                                        onCheckedChange = { checked ->
                                            if (checked == true) selectedProductSkus.add(prod.sku)
                                            else selectedProductSkus.remove(prod.sku)
                                        }
                                    )
                                    Text("${prod.name} (${prod.sku})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    if (mode != "CHOICE") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    try {
                                        val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                        val token = "Bearer ${configManager.authToken}"

                                        when (mode) {
                                            "ISBN" -> {
                                                api.ingestIsbn(token, IngestIsbnRequest(
                                                    isbn = isbn,
                                                    quantity = quantity.toIntOrNull() ?: 1,
                                                    price = price.toDoubleOrNull()
                                                ))
                                            }
                                            "MANUAL" -> {
                                                val attrs = ProductAttributes(
                                                    type = if (itemType == "BOOK") "BOOK" else "PENCIL",
                                                    author = if (itemType == "BOOK") author else null,
                                                    brand = if (itemType == "STATIONERY") brand else null
                                                )
                                                api.createProduct(token, CreateProductRequest(
                                                    sku = sku,
                                                    name = name,
                                                    basePrice = price.toDoubleOrNull() ?: 0.0,
                                                    type = itemType,
                                                    attributes = attrs
                                                ))
                                            }
                                            "BUNDLE" -> {
                                                api.createBundle(token, BundleDTO(
                                                    sku = bundleSku,
                                                    name = bundleName,
                                                    price = bundlePrice.toDoubleOrNull() ?: 0.0,
                                                    bundledProductSkus = selectedProductSkus.toList()
                                                ))
                                            }
                                        }
                                        Toast.makeText(context, "Created successfully!", Toast.LENGTH_SHORT).show()
                                        onProductAdded()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = !isSubmitting
                        ) {
                            Text(if (isSubmitting) "Saving..." else "Submit")
                        }
                    }
                }
            }
        }
    }
}
