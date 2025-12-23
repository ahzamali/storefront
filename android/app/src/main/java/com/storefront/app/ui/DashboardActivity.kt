package com.storefront.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.storefront.app.ConfigManager
import com.storefront.app.viewmodel.CartViewModel

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configManager = ConfigManager(this)

        setContent {
            MaterialTheme {
                DashboardScreen(configManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(configManager: ConfigManager) {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val context = LocalContext.current
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StoreFront") },
                actions = {
                    IconButton(onClick = { 
                        context.startActivity(Intent(context, UserManagementActivity::class.java)) 
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "User Management")
                    }
                    IconButton(onClick = {
                        configManager.clearAuth()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Logout") // Using Settings icon for Logout for now
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "POS") },
                    label = { Text("POS") },
                    selected = currentRoute == "pos",
                    onClick = { navController.navigate("pos") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    selected = currentRoute == "inventory",
                    onClick = { navController.navigate("inventory") }
                )
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
        }
    }
}
