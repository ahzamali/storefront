package com.storefront.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.storefront.app.ConfigManager
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var configManager: ConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configManager = ConfigManager(this)

        // Session check: If already logged in, redirect directly to Dashboard
        if (configManager.isLoggedIn) {
            val destination = if (configManager.selectedStoreId != null) {
                DashboardActivity::class.java
            } else {
                StoreSelectionActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginScreen()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen() {
        var serverUrl by remember { mutableStateOf(configManager.baseUrl) }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showAdvancedSettings by remember { mutableStateOf(false) }
        var expandedHistory by remember { mutableStateOf(false) }
        var historyList by remember { mutableStateOf(configManager.serverHistory.toList()) }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "StoreFront", 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Point of Sale & Inventory Management", 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            var passwordVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { 
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(this@LoginActivity, "Please enter username and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val urlToUse = serverUrl.ifBlank { ConfigManager.DEFAULT_BASE_URL }
                    isLoading = true
                    performLogin(urlToUse, username.trim(), password) { isLoading = false }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Log In", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expandable Advanced Server URL Settings
            Row(
                modifier = Modifier
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Server Settings (${if (showAdvancedSettings) "Hide" else "Customize URL"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Icon(
                    imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedHistory,
                        onExpandedChange = { expandedHistory = !expandedHistory }
                    ) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHistory) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedHistory,
                            onDismissRequest = { expandedHistory = false }
                        ) {
                            historyList.forEach { url ->
                                DropdownMenuItem(
                                    text = { Text(url) },
                                    onClick = {
                                        serverUrl = url
                                        expandedHistory = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = "Default: ${ConfigManager.DEFAULT_BASE_URL}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }

    private fun performLogin(url: String, user: String, pass: String, onComplete: () -> Unit) {
        val cleanUrl = url.trim().removeSuffix("/")
        lifecycleScope.launch {
            try {
                val api = NetworkModule.createApiService(cleanUrl)
                val response = api.login(mapOf("username" to user, "password" to pass))
                
                val token = response["token"] as? String 
                
                if (token != null) {
                    configManager.baseUrl = cleanUrl
                    configManager.authToken = token
                    configManager.username = user
                    configManager.userRole = response["role"] as? String
                    val idVal = (response["userId"] as? Number)?.toLong()
                    if (idVal != null) configManager.userId = idVal
                    
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    
                    startActivity(Intent(this@LoginActivity, StoreSelectionActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login Failed: Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Login Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                onComplete()
            }
        }
    }
}
