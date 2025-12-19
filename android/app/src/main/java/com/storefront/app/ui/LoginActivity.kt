package com.storefront.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        var serverUrl by remember { mutableStateOf(configManager.baseUrl ?: "http://10.0.2.2:8080") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        var historyList by remember { mutableStateOf(configManager.serverHistory.toList()) }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "StoreFront Login", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(32.dp))

            // Server URL with History Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    historyList.forEach { url ->
                        DropdownMenuItem(
                            text = { Text(url) },
                            onClick = {
                                serverUrl = url
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (serverUrl.isBlank()) {
                         Toast.makeText(this@LoginActivity, "Please enter Server URL", Toast.LENGTH_SHORT).show()
                         return@Button
                    }
                    isLoading = true
                    performLogin(serverUrl, username, password) { isLoading = false }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login")
                }
            }
        }
    }

    private fun performLogin(url: String, user: String, pass: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val api = NetworkModule.createApiService(url)
                val response = api.login(mapOf("username" to user, "password" to pass))
                
                val token = response["token"] as? String 
                
                if (token != null) {
                    configManager.baseUrl = url 
                    configManager.authToken = token
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    
                    startActivity(Intent(this@LoginActivity, StoreSelectionActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login Failed: No Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                onComplete()
            }
        }
    }
}
