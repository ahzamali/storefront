package com.storefront.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storefront.app.ConfigManager
import com.storefront.app.model.CreateOrderRequest
import com.storefront.app.model.OrderItemRequest
import com.storefront.app.model.ProductStockDTO
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

    private var _customerName: String? = null
    private var _customerPhone: String? = null

    val totalAmount: BigDecimal
        get() = _cartItems.fold(BigDecimal.ZERO) { acc, item -> 
            acc.add(item.price.multiply(BigDecimal(item.quantity))) 
        }
    
    fun setCustomer(name: String, phone: String) {
        _customerName = name.ifBlank { null }
        _customerPhone = phone.ifBlank { null }
    }

    fun addToCart(product: ProductStockDTO, quantity: Int = 1) {
        val sku = product.sku
        val name = product.name
        val price = BigDecimal(product.basePrice.toString())
        val isBundle = product.type == "BUNDLE"
        
        val existingIndex = _cartItems.indexOfFirst { it.sku == sku && it.excludedSkus.isEmpty() }
        
        if (existingIndex != -1) {
            val existing = _cartItems[existingIndex]
            _cartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            _cartItems.add(CartItem(sku, name, price, quantity, isBundle))
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
        _customerName = null
        _customerPhone = null
    }

    fun checkout(configManager: ConfigManager, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val baseUrl = configManager.baseUrl ?: return
        val token = configManager.authToken ?: return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.createApiService(baseUrl)
                
                // Use selected store from config, or default to 1 (Master) if not set, or throw error
                // Ideally login flow sets this.
                val storeId = configManager.selectedStoreId ?: 1 
                
                val itemsPayload = _cartItems.map { 
                    OrderItemRequest(it.sku, it.quantity)
                    // Note: Excluded Products not fully supported in simple request yet in this new DTO unless we add it
                    // The backend OrderItemRequestDTO likely has excludedProductSkus.
                    // For now, simplicity.
                }
                
                val orderRequest = CreateOrderRequest(
                    customerName = _customerName,
                    customerPhone = _customerPhone,
                    storeId = storeId,
                    items = itemsPayload
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
