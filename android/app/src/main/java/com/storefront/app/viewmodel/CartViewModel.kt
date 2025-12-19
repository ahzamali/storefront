package com.storefront.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storefront.app.ConfigManager
import com.storefront.app.network.NetworkModule
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CartItem(
    val sku: String,
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
    val isBundle: Boolean = false,
    val excludedSkus: List<String> = emptyList()
)

class CartViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> get() = _cartItems

    val totalAmount: BigDecimal
        get() = _cartItems.fold(BigDecimal.ZERO) { acc, item -> 
            acc.add(item.price.multiply(BigDecimal(item.quantity))) 
        }

    fun addToCart(product: Map<String, Any>, quantity: Int = 1) {
        // Simplified Logic: accepting Map from API for now
        // Ideally we map this to a Product model
        val sku = product["sku"] as String
        val name = product["name"] as String
        val price = BigDecimal(product["price"].toString()) // or basePrice
        
        // Check if exists (simple check, ignoring exclusions/bundles complexity for now)
        val existingIndex = _cartItems.indexOfFirst { it.sku == sku && it.excludedSkus.isEmpty() }
        
        if (existingIndex != -1) {
            val existing = _cartItems[existingIndex]
            _cartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            _cartItems.add(CartItem(sku, name, price, quantity))
        }
    }
    
    fun removeOne(item: CartItem) {
        val index = _cartItems.indexOf(item)
        if (index != -1) {
            if (item.quantity > 1) {
                _cartItems[index] = item.copy(quantity = item.quantity - 1)
            } else {
                _cartItems.removeAt(index)
            }
        }
    }

    fun clearCart() {
        _cartItems.clear()
    }

    fun checkout(configManager: ConfigManager, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val baseUrl = configManager.baseUrl ?: return
        val token = configManager.authToken ?: return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.createApiService(baseUrl)
                
                // Construct Order Request (ignoring storeId for now, need to fetch virtual store ID?)
                // Assumption: User is linked to a Store, or we pick the store ID from context/selection.
                // For this MVP, let's hardcode storeId or ask user?
                // Better: The API 'createOrder' requires storeId.
                // We'll pass a dummy '1' or add store selection to settings.
                // Let's assume storeId = 1 for now.
                
                val itemsPayload = _cartItems.map { 
                    mapOf(
                        "sku" to it.sku,
                        "quantity" to it.quantity,
                        "excludedProductSkus" to it.excludedSkus
                    ) 
                }
                
                val orderRequest = mapOf(
                    "storeId" to configManager.selectedStoreId,
                    "items" to itemsPayload
                )

                api.createOrder("Bearer $token", orderRequest)
                clearCart()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Checkout Failed")
            }
        }
    }
}
