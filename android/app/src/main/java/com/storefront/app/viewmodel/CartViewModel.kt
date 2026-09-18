package com.storefront.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storefront.app.ConfigManager
import com.storefront.app.model.*
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

    var lastCompletedOrder: OrderDTO? = null
        private set

    val totalAmount: BigDecimal
        get() = _cartItems.fold(BigDecimal.ZERO) { acc, item -> 
            acc.add(item.price.multiply(BigDecimal(item.quantity))) 
        }

    val customerName: String? get() = _customerName
    val customerPhone: String? get() = _customerPhone
    
    fun setCustomer(name: String, phone: String) {
        _customerName = name.ifBlank { null }
        _customerPhone = phone.ifBlank { null }
    }

    fun addToCart(product: ProductStockDTO, quantity: Int = 1, excludedSkus: List<String> = emptyList()) {
        val sku = product.sku
        val name = product.name
        val price = BigDecimal(product.price.toString())
        val isBundle = product.type == "BUNDLE"
        
        val existingIndex = _cartItems.indexOfFirst { it.sku == sku && it.excludedSkus == excludedSkus }
        
        if (existingIndex != -1) {
            val existing = _cartItems[existingIndex]
            _cartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            _cartItems.add(CartItem(sku, name, price, quantity, isBundle, excludedSkus))
        }
    }

    fun incrementQuantity(item: CartItem) {
        val index = _cartItems.indexOf(item)
        if (index != -1) {
            _cartItems[index] = item.copy(quantity = item.quantity + 1)
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

    fun removeItem(item: CartItem) {
        _cartItems.remove(item)
    }

    fun clearCart() {
        _cartItems.clear()
        _customerName = null
        _customerPhone = null
    }

    fun checkout(configManager: ConfigManager, onSuccess: (OrderDTO) -> Unit, onError: (String) -> Unit) {
        val baseUrl = configManager.baseUrl ?: return
        val token = configManager.authToken ?: return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.createApiService(baseUrl)
                val storeId = configManager.selectedStoreId ?: 1L
                
                val itemsPayload = _cartItems.map { 
                    OrderItemRequest(
                        sku = it.sku, 
                        quantity = it.quantity,
                        excludedProductSkus = it.excludedSkus
                    )
                }
                
                val orderRequest = CreateOrderRequest(
                    customerName = _customerName,
                    customerPhone = _customerPhone,
                    storeId = storeId,
                    items = itemsPayload
                )

                val order = api.createOrder("Bearer $token", orderRequest)
                lastCompletedOrder = order
                clearCart()
                onSuccess(order)
            } catch (e: Exception) {
                onError(e.message ?: "Checkout Failed")
            }
        }
    }
}
