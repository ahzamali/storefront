package com.storefront.app.model

data class ProductAttributes(
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val brand: String? = null,
    val hardness: String? = null,
    val material: String? = null,
    val type: String? = null, // JSON Discriminator: BOOK, PENCIL
    val eraserIncluded: Boolean? = null
)

data class CreateProductRequest(
    val sku: String,
    val name: String,
    val basePrice: Double,
    val type: String, // Top level type: BOOK, STATIONERY
    val attributes: ProductAttributes
)

data class ProductStockDTO(
    val id: Long,
    val sku: String,
    val name: String,
    val type: String, // BOOK, PENCIL, BUNDLE
    val price: Double,
    val quantity: Int, // Available stock
    val attributes: ProductAttributes? = null
)

data class Store(
    val id: Long,
    val name: String,
    val type: String, // PHYSICAL, VIRTUAL, MASTER
    val location: String? = null
)

enum class Role {
    SUPER_ADMIN, STORE_ADMIN, EMPLOYEE, CUSTOMER
}

data class AppUser(
    val id: Long,
    val username: String,
    val role: Role,
    val stores: List<Store> = emptyList()
)

data class CreateUserRequest(
    val username: String,
    val password: String,
    val role: Role,
    val storeId: Long?
)

data class CreateOrderRequest(
    val customerName: String?,
    val customerPhone: String?,
    val storeId: Long?,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val sku: String,
    val quantity: Int
)
