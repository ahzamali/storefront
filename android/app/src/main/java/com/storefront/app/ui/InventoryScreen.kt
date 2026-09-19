package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import com.storefront.app.network.GoogleBooksClient
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
        RestockDialog(
            product = product,
            configManager = configManager,
            onDismiss = { showRestockDialogFor = null },
            onStockAdded = {
                showRestockDialogFor = null
                loadInventory(selectedStore?.id)
            }
        )
    }

    // Edit Product Dialog
    showEditDialogFor?.let { product ->
        EditProductDialog(
            product = product,
            selectedStoreId = selectedStore?.id,
            configManager = configManager,
            onDismiss = { showEditDialogFor = null },
            onProductUpdated = {
                showEditDialogFor = null
                loadInventory(selectedStore?.id)
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

    // Manual / Common fields
    var isbn by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var itemType by remember { mutableStateOf("BOOK") }
    var author by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }

    // ISBN lookup state
    var isLookingUpBook by remember { mutableStateOf(false) }
    var bookFoundOnline by remember { mutableStateOf<Boolean?>(null) }
    var lookupMessage by remember { mutableStateOf<String?>(null) }

    // Bundle fields
    var bundleSku by remember { mutableStateOf("") }
    var bundleName by remember { mutableStateOf("") }
    var bundlePrice by remember { mutableStateOf("") }
    val selectedProductSkus = remember { mutableStateListOf<String>() }

    fun lookupBookDetails(rawIsbn: String) {
        val clean = rawIsbn.trim().replace("-", "")
        if (clean.isBlank()) {
            Toast.makeText(context, "Please enter an ISBN first", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isLookingUpBook = true
            lookupMessage = null
            try {
                // 1. Check local loaded products first
                val localMatch = products.find { it.sku.replace("-", "").equals(clean, ignoreCase = true) }
                if (localMatch != null) {
                    name = localMatch.name
                    author = localMatch.attributes?.author ?: ""
                    if (localMatch.price > 0 && price.isBlank()) {
                        price = localMatch.price.toString()
                    }
                    bookFoundOnline = true
                    lookupMessage = "Book already exists in inventory."
                    return@launch
                }

                // 2. Query Google Books API
                val response = GoogleBooksClient.service.searchByIsbn("isbn:$clean")
                val item = response.items?.firstOrNull()
                if (item != null && item.volumeInfo != null) {
                    val info = item.volumeInfo
                    name = info.title ?: ""
                    author = info.authors?.joinToString(", ") ?: ""
                    val listPrice = item.saleInfo?.listPrice?.amount
                    if (listPrice != null && listPrice > 0 && price.isBlank()) {
                        price = listPrice.toString()
                    }
                    bookFoundOnline = true
                    lookupMessage = "Found: ${info.title ?: "Book details"}"
                } else {
                    bookFoundOnline = false
                    lookupMessage = "Book title not found online. Please enter book name manually."
                }
            } catch (e: Exception) {
                bookFoundOnline = false
                lookupMessage = "Could not fetch book details online. Please enter book name manually."
            } finally {
                isLookingUpBook = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
                                                lookupBookDetails(scanned)
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
                            Text("Scan Book Barcode")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = isbn, 
                            onValueChange = { 
                                isbn = it
                                bookFoundOnline = null
                                lookupMessage = null
                            }, 
                            label = { Text("ISBN (10 or 13 digits) *") }, 
                            singleLine = true, 
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { lookupBookDetails(isbn) },
                                        enabled = !isLookingUpBook && isbn.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Lookup Book")
                                    }
                                    IconButton(onClick = {
                                        try {
                                            val scanner = GmsBarcodeScanning.getClient(context)
                                            scanner.startScan()
                                                .addOnSuccessListener { barcode ->
                                                    barcode.rawValue?.let { scanned ->
                                                        isbn = scanned
                                                        lookupBookDetails(scanned)
                                                    }
                                                }
                                        } catch (e: Exception) {}
                                    }) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isLookingUpBook) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Searching book details...", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        lookupMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = if (bookFoundOnline == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (bookFoundOnline == true) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (bookFoundOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        msg, 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (bookFoundOnline == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name, 
                            onValueChange = { name = it }, 
                            label = { Text("Book Title *") }, 
                            placeholder = { Text(if (bookFoundOnline == false) "Enter book title manually" else "Book Title") },
                            singleLine = true, 
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = author, 
                            onValueChange = { author = it }, 
                            label = { Text("Author (Optional)") }, 
                            singleLine = true, 
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = quantity, 
                            onValueChange = { quantity = it }, 
                            label = { Text("Quantity") }, 
                            singleLine = true, 
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = price, 
                            onValueChange = { price = it }, 
                            label = { Text("Price (₹)") }, 
                            singleLine = true, 
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                                val cleanIsbn = isbn.trim()
                                                if (cleanIsbn.isBlank()) {
                                                    Toast.makeText(context, "Please enter or scan an ISBN", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }
                                                if (name.isBlank()) {
                                                    Toast.makeText(context, "Please enter a book name", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                val qty = quantity.toIntOrNull() ?: 1
                                                val p = price.toDoubleOrNull()

                                                try {
                                                    api.ingestIsbn(token, IngestIsbnRequest(
                                                        isbn = cleanIsbn,
                                                        name = name.trim().ifBlank { null },
                                                        author = author.trim().ifBlank { null },
                                                        quantity = qty,
                                                        price = p
                                                    ))
                                                } catch (e: Exception) {
                                                    // Fallback to direct create product + add stock
                                                    val attrs = ProductAttributes(
                                                        type = "BOOK",
                                                        isbn = cleanIsbn,
                                                        author = author.trim().ifBlank { null }
                                                    )
                                                    api.createProduct(token, CreateProductRequest(
                                                        sku = cleanIsbn,
                                                        name = name.trim(),
                                                        basePrice = p ?: 0.0,
                                                        type = "BOOK",
                                                        attributes = attrs
                                                    ))
                                                    if (qty > 0) {
                                                        api.addStock(token, AddStockRequest(sku = cleanIsbn, quantity = qty))
                                                    }
                                                }
                                            }
                                            "MANUAL" -> {
                                                val cleanSku = sku.trim()
                                                val cleanName = name.trim()
                                                if (cleanSku.isBlank()) {
                                                    Toast.makeText(context, "Please enter a product SKU", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }
                                                if (cleanName.isBlank()) {
                                                    Toast.makeText(context, "Please enter a product name", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                val initialPrice = price.toDoubleOrNull() ?: 0.0
                                                val initialQty = quantity.toIntOrNull() ?: 1
                                                val attrs = ProductAttributes(
                                                    type = if (itemType == "BOOK") "BOOK" else "PENCIL",
                                                    author = if (itemType == "BOOK") author.trim().ifBlank { null } else null,
                                                    brand = if (itemType == "STATIONERY") brand.trim().ifBlank { null } else null
                                                )
                                                api.createProduct(token, CreateProductRequest(
                                                    sku = cleanSku,
                                                    name = cleanName,
                                                    basePrice = initialPrice,
                                                    type = itemType,
                                                    attributes = attrs
                                                ))
                                                if (initialQty > 0) {
                                                    try {
                                                        api.addStock(token, AddStockRequest(sku = cleanSku, quantity = initialQty))
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                            "BUNDLE" -> {
                                                val cleanSku = bundleSku.trim()
                                                val cleanName = bundleName.trim()
                                                if (cleanSku.isBlank()) {
                                                    Toast.makeText(context, "Please enter a bundle SKU", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }
                                                if (cleanName.isBlank()) {
                                                    Toast.makeText(context, "Please enter a bundle name", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }
                                                if (selectedProductSkus.isEmpty()) {
                                                    Toast.makeText(context, "Please select at least one bundled product", Toast.LENGTH_SHORT).show()
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                api.createBundle(token, BundleDTO(
                                                    sku = cleanSku,
                                                    name = cleanName,
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

@Composable
fun RestockDialog(
    product: ProductStockDTO,
    configManager: ConfigManager,
    onDismiss: () -> Unit,
    onStockAdded: () -> Unit
) {
    var restockQuantity by remember(product.id) { mutableStateOf("10") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stock - ${product.name}") },
        text = {
            Column {
                Text("Current stock: ${product.quantity}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = restockQuantity,
                    onValueChange = { restockQuantity = it },
                    label = { Text("Quantity to Add") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = restockQuantity.toIntOrNull() ?: 10
                    if (qty <= 0) {
                        Toast.makeText(context, "Please enter a valid quantity greater than 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        isSaving = true
                        try {
                            val api = NetworkModule.createApiService(configManager.baseUrl!!)
                            api.addStock("Bearer ${configManager.authToken}", AddStockRequest(product.sku, qty))
                            Toast.makeText(context, "Stock added successfully!", Toast.LENGTH_SHORT).show()
                            onStockAdded()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Adding..." else "Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditProductDialog(
    product: ProductStockDTO,
    selectedStoreId: Long?,
    configManager: ConfigManager,
    onDismiss: () -> Unit,
    onProductUpdated: () -> Unit
) {
    var editName by remember(product.id) { mutableStateOf(product.name) }
    var editPrice by remember(product.id) { 
        mutableStateOf(if (product.price > 0) product.price.toString() else "0") 
    }
    var editQuantity by remember(product.id) { mutableStateOf(product.quantity.toString()) }
    var editAuthor by remember(product.id) { mutableStateOf(product.attributes?.author ?: "") }
    var editBrand by remember(product.id) { mutableStateOf(product.attributes?.brand ?: "") }
    var editIsbn by remember(product.id) { mutableStateOf(product.attributes?.isbn ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = editName, 
                    onValueChange = { editName = it }, 
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editPrice, 
                    onValueChange = { editPrice = it }, 
                    label = { Text("Price (₹) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (product.type != "BUNDLE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editQuantity, 
                        onValueChange = { editQuantity = it }, 
                        label = { Text("Stock Quantity (Current: ${product.quantity})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (product.type == "BOOK" || editAuthor.isNotBlank() || editIsbn.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAuthor, 
                        onValueChange = { editAuthor = it }, 
                        label = { Text("Author") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editIsbn, 
                        onValueChange = { editIsbn = it }, 
                        label = { Text("ISBN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (product.type == "STATIONERY" || editBrand.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBrand, 
                        onValueChange = { editBrand = it }, 
                        label = { Text("Brand") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = editName.trim().ifBlank { product.name }
                    val finalPrice = editPrice.toDoubleOrNull() ?: product.price
                    val finalQuantity = editQuantity.toIntOrNull() ?: product.quantity

                    if (finalName.isBlank()) {
                        Toast.makeText(context, "Product name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        isSaving = true
                        try {
                            val api = NetworkModule.createApiService(configManager.baseUrl!!)
                            val token = "Bearer ${configManager.authToken}"

                            val updatedAttrs = (product.attributes ?: ProductAttributes()).copy(
                                author = editAuthor.trim().ifBlank { product.attributes?.author },
                                brand = editBrand.trim().ifBlank { product.attributes?.brand },
                                isbn = editIsbn.trim().ifBlank { product.attributes?.isbn }
                            )

                            val req = CreateProductRequest(
                                sku = product.sku,
                                name = finalName,
                                basePrice = finalPrice,
                                type = product.type,
                                attributes = updatedAttrs
                            )
                            api.updateProduct(token, product.id, req)

                            // If stock quantity changed for non-bundle, update stock count
                            if (product.type != "BUNDLE" && finalQuantity != product.quantity) {
                                api.updateStockCount(token, UpdateStockRequest(
                                    sku = product.sku,
                                    quantity = finalQuantity,
                                    storeId = selectedStoreId
                                ))
                            }

                            Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
                            onProductUpdated()
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
