package com.storefront.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.storefront.app.ConfigManager
import com.storefront.app.viewmodel.CartViewModel

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configManager = ConfigManager(this)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val cartViewModel: CartViewModel = viewModel()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("POS") },
                                selected = false, // TODO: state
                                onClick = { navController.navigate("pos") }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                                label = { Text("Inventory") },
                                selected = false,
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
        }
    }
}
