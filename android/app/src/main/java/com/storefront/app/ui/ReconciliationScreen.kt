package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
fun ReconciliationScreen(configManager: ConfigManager) {
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var reconciliationLogs by remember { mutableStateOf<List<ReconciliationLogDTO>>(emptyList()) }
    var lastReport by remember { mutableStateOf<ReconciliationReportDTO?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showReconcileConfirmDialog by remember { mutableStateOf(false) }

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
                
                val currentStoreId = selectedStore?.id ?: stores.firstOrNull { it.type != "MASTER" }?.id
                if (currentStoreId != null) {
                    reconciliationLogs = api.getReconciliationHistory(token, currentStoreId)
                }
            } catch (e: Exception) {
                // Ignore empty / 404 logs
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    LaunchedEffect(selectedStore) {
        selectedStore?.let { store ->
            scope.launch {
                try {
                    val api = NetworkModule.createApiService(configManager.baseUrl!!)
                    reconciliationLogs = api.getReconciliationHistory("Bearer ${configManager.authToken}", store.id)
                } catch (e: Exception) {}
            }
        }
    }

    val virtualStores = stores.filter { it.type != "MASTER" }
    val activeStore = selectedStore ?: virtualStores.firstOrNull()

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Store Reconciliation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Audit daily sales & finalize store shift", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(selectedStore?.name ?: virtualStores.firstOrNull()?.name ?: "Select Store")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        virtualStores.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expanded = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Card
            if (activeStore != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Store: ${activeStore.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Triggering reconciliation will calculate total sales revenue and optionally return remaining stock to Master Store.", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showReconcileConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Perform End-of-Day Reconciliation")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Reconciliation Audit Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reconciliationLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past reconciliation logs found for this store.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(reconciliationLogs) { log ->
                        ReconciliationLogCard(log = log)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Confirmation & Option Dialog
    if (showReconcileConfirmDialog && activeStore != null) {
        var returnStockToHQ by remember { mutableStateOf(false) }
        var isReconciling by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showReconcileConfirmDialog = false },
            title = { Text("Confirm Reconciliation") },
            text = {
                Column {
                    Text("Reconcile sales for '${activeStore.name}'?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = returnStockToHQ, onCheckedChange = { returnStockToHQ = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Return unsold stock back to HQ / Master Store", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isReconciling = true
                            try {
                                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                val report = api.reconcileStore("Bearer ${configManager.authToken}", activeStore.id, returnStockToHQ)
                                lastReport = report
                                Toast.makeText(context, "Reconciliation Completed!", Toast.LENGTH_SHORT).show()
                                showReconcileConfirmDialog = false
                                loadData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isReconciling = false
                            }
                        }
                    },
                    enabled = !isReconciling
                ) {
                    Text(if (isReconciling) "Reconciling..." else "Confirm & Reconcile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReconcileConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reconciliation Summary Dialog
    lastReport?.let { report ->
        ReconciliationReportModal(report = report, onDismiss = { lastReport = null })
    }
}

@Composable
fun ReconciliationLogCard(log: ReconciliationLogDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Reconciliation #${log.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!log.reconciledAt.isNullOrBlank()) {
                    Text("Date: ${log.reconciledAt}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    if (log.itemsReturned) "Stock Returned to HQ: Yes" else "Stock Returned: No", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = if (log.itemsReturned) MaterialTheme.colorScheme.primary else Color.DarkGray
                )
            }
            Text(
                "₹${log.totalSalesValue}", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReconciliationReportModal(report: ReconciliationReportDTO, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Shift Reconciliation Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Store: ${report.storeName ?: "Virtual Store"}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Orders Processed:", fontWeight = FontWeight.SemiBold)
                    Text("${report.totalOrders}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Gross Sales Revenue:", fontWeight = FontWeight.SemiBold)
                    Text("₹${report.totalGrossRevenue}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                if (report.itemsSummary.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Items Sold & Balances:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(report.itemsSummary) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.soldQuantity} sold • ${item.productName}", maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("₹${item.totalRevenue}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close Report")
                }
            }
        }
    }
}
