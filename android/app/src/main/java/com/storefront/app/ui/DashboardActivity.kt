package com.storefront.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storefront.app.ConfigManager
import com.storefront.app.model.Store
import com.storefront.app.network.NetworkModule
import com.storefront.app.viewmodel.CartViewModel
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configManager = ConfigManager(this)

        if (!configManager.isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContent {
            MaterialTheme {
                DashboardScreen(configManager)
            }
        }
    }
}

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(configManager: ConfigManager) {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "pos"

    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var currentStoreName by remember { mutableStateOf("HQ (Master)") }
    var showStoreSwitcherDialog by remember { mutableStateOf(false) }

    fun refreshStores() {
        scope.launch {
            try {
                val baseUrl = configManager.baseUrl
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                stores = api.getStores(token)
                val selId = configManager.selectedStoreId
                val found = stores.find { it.id == selId }
                currentStoreName = found?.name ?: "HQ (Master)"
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        refreshStores()
    }

    val userRole = configManager.userRole ?: "EMPLOYEE"
    val isSuperAdmin = userRole.contains("SUPER_ADMIN") || userRole.contains("ADMIN")
    val isStoreAdmin = isSuperAdmin || userRole.contains("STORE_ADMIN")

    // Dynamic Navigation items based on RBAC
    val navItems = remember(userRole) {
        buildList {
            add(NavItem("pos", "POS", Icons.Default.ShoppingCart))
            add(NavItem("inventory", "Inventory", Icons.Default.List))
            if (isStoreAdmin) {
                add(NavItem("orders", "Orders", Icons.Default.Receipt))
                add(NavItem("reconciliation", "Reconcile", Icons.Default.Assessment))
            }
            if (isSuperAdmin) {
                add(NavItem("stores", "Stores", Icons.Default.Store))
                add(NavItem("users", "Users", Icons.Default.People))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "StoreFront", 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Store: $currentStoreName • ${configManager.username ?: userRole}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Store Switcher button
                    IconButton(onClick = { showStoreSwitcherDialog = true }) {
                        Icon(Icons.Default.Storefront, contentDescription = "Switch Store")
                    }
                    // Logout button
                    IconButton(onClick = {
                        configManager.clearAuth()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            // Horizontal Scrollable Bottom Navigation Bar - Prevents character wrapping
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "pos",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("pos") { PosScreen(configManager, cartViewModel) }
            composable("inventory") { InventoryScreen(configManager) }
            composable("orders") { OrdersScreen(configManager) }
            composable("stores") { StoreManagerScreen(configManager) }
            composable("reconciliation") { ReconciliationScreen(configManager) }
            composable("users") { UserManagementScreen(configManager) }
        }
    }

    // Store Switcher Dialog
    if (showStoreSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showStoreSwitcherDialog = false },
            title = { Text("Switch Active Store") },
            text = {
                Column {
                    Text("Select current active store for this session:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    stores.forEach { s ->
                        OutlinedButton(
                            onClick = {
                                configManager.selectedStoreId = s.id
                                currentStoreName = s.name
                                showStoreSwitcherDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = if (configManager.selectedStoreId == s.id) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(
                                text = "${s.name} (${s.type})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStoreSwitcherDialog = false }) { Text("Close") }
            }
        )
    }
}
