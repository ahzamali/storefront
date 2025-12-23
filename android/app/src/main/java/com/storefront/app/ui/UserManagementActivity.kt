package com.storefront.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.storefront.app.ConfigManager
import com.storefront.app.model.*
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

class UserManagementActivity : ComponentActivity() {
    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager(this)

        setContent {
            MaterialTheme {
                UserManagementScreen(configManager) { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(configManager: ConfigManager, onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                val api = NetworkModule.createApiService(configManager.baseUrl!!)
                val token = "Bearer ${configManager.authToken}"
                users = api.getUsers(token)
                stores = api.getStores(token)
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("User Management") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(users) { user ->
                        UserCard(user, onDelete = {
                            scope.launch {
                                try {
                                    val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                    api.deleteUser("Bearer ${configManager.authToken}", user.id)
                                    loadData()
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            stores = stores,
            onDismiss = { showAddDialog = false },
            onAdd = { req ->
                scope.launch {
                    try {
                        val api = NetworkModule.createApiService(configManager.baseUrl!!)
                        api.register("Bearer ${configManager.authToken}", req)
                        showAddDialog = false
                        loadData()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
fun UserCard(user: AppUser, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(user.username, style = MaterialTheme.typography.titleMedium)
                Text("Role: ${user.role}", style = MaterialTheme.typography.bodyMedium)
                if (user.stores.isNotEmpty()) {
                    Text("Stores: ${user.stores.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddUserDialog(stores: List<Store>, onDismiss: () -> Unit, onAdd: (CreateUserRequest) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.EMPLOYEE) }
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var expandedRole by remember { mutableStateOf(false) }
    var expandedStore by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add User", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
                Spacer(modifier = Modifier.height(8.dp))

                // Role Dropdown
                Box {
                    Button(onClick = { expandedRole = true }) { Text("Role: $role") }
                    DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                        Role.values().forEach { r ->
                            DropdownMenuItem(text = { Text(r.name) }, onClick = { role = r; expandedRole = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Store Dropdown
                if (role != Role.SUPER_ADMIN) {
                    Box {
                        Button(onClick = { expandedStore = true }) { Text(selectedStore?.name ?: "Select Store") }
                        DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                            stores.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expandedStore = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    onAdd(CreateUserRequest(username, password, role, selectedStore?.id))
                }) {
                    Text("Create")
                }
            }
        }
    }
}
