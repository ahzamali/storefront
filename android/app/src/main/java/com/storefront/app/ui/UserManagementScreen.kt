package com.storefront.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
fun UserManagementScreen(configManager: ConfigManager, onBack: (() -> Unit)? = null) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editUserFor by remember { mutableStateOf<AppUser?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                val baseUrl = configManager.baseUrl ?: return@launch
                val token = "Bearer ${configManager.authToken}"
                val api = NetworkModule.createApiService(baseUrl)
                users = api.getUsers(token)
                stores = api.getStores(token)
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("User & Access Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Manage employees, store admins, and store access", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onEdit = { editUserFor = user },
                            onDelete = {
                                scope.launch {
                                    try {
                                        val api = NetworkModule.createApiService(configManager.baseUrl!!)
                                        api.deleteUser("Bearer ${configManager.authToken}", user.id)
                                        Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
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
                        Toast.makeText(context, "User created successfully!", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    editUserFor?.let { user ->
        EditUserDialog(
            user = user,
            stores = stores,
            onDismiss = { editUserFor = null },
            onSave = { updateDto ->
                scope.launch {
                    try {
                        val api = NetworkModule.createApiService(configManager.baseUrl!!)
                        api.updateUser("Bearer ${configManager.authToken}", user.id, updateDto)
                        Toast.makeText(context, "User updated successfully!", Toast.LENGTH_SHORT).show()
                        editUserFor = null
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun UserCard(user: AppUser, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "${user.role}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (user.stores.isNotEmpty()) {
                    Text(
                        "Assigned Stores: ${user.stores.joinToString { it.name }}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.DarkGray
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
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
                Text("Create User Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                // Role Dropdown
                Box {
                    OutlinedButton(onClick = { expandedRole = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Role: $role")
                    }
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
                        OutlinedButton(onClick = { expandedStore = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedStore?.name ?: "Assign Initial Store")
                        }
                        DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                            stores.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStore = s; expandedStore = false })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(CreateUserRequest(username, password, role, selectedStore?.id)) },
                        enabled = username.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserDialog(
    user: AppUser,
    stores: List<Store>,
    onDismiss: () -> Unit,
    onSave: (UserUpdateDTO) -> Unit
) {
    var newPassword by remember(user.id) { mutableStateOf("") }
    var selectedRole by remember(user.id) { mutableStateOf(user.role.name) }
    var expandedRole by remember { mutableStateOf(false) }
    val assignedStoreIds = remember(user.id) { mutableStateListOf<Long>().apply { addAll(user.stores.map { it.id }) } }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit User: ${user.username}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (leave blank to keep)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    OutlinedButton(onClick = { expandedRole = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Role: $selectedRole")
                    }
                    DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                        Role.values().forEach { r ->
                            DropdownMenuItem(text = { Text(r.name) }, onClick = { selectedRole = r.name; expandedRole = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Assigned Stores:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 160.dp)) {
                    items(stores) { store ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = assignedStoreIds.contains(store.id),
                                onCheckedChange = { checked ->
                                    if (checked == true) assignedStoreIds.add(store.id)
                                    else assignedStoreIds.remove(store.id)
                                }
                            )
                            Text(store.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(UserUpdateDTO(
                            password = newPassword.ifBlank { null },
                            storeIds = assignedStoreIds.toList(),
                            role = selectedRole
                        ))
                    }) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
