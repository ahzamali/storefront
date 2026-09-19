package com.storefront.app.model

import com.google.gson.annotations.SerializedName

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

data class UpdateProductRequest(
    val sku: String? = null,
    val name: String? = null,
    val basePrice: Double? = null,
    val type: String? = null,
    val attributes: ProductAttributes? = null
)

data class ProductStockDTO(
    val id: Long,
    val sku: String,
    val name: String,
    val type: String, // BOOK, PENCIL, BUNDLE, STATIONERY
    val price: Double,
    val quantity: Int, // Available stock
    val attributes: ProductAttributes? = null,
    val bundledProducts: List<BundleItemDetailDTO>? = null
)

data class BundleItemDetailDTO(
    val sku: String,
    val name: String,
    val quantity: Int = 1
)

data class BundleDTO(
    val sku: String,
    val name: String,
    val price: Double,
    val bundledProductSkus: List<String> = emptyList()
)

data class IngestIsbnRequest(
    val isbn: String,
    val name: String? = null,
    val author: String? = null,
    val quantity: Int = 1,
    val price: Double? = null
)

// Google Books API Models
data class GoogleBooksResponse(
    val totalItems: Int? = 0,
    val items: List<GoogleBookItem>? = null
)

data class GoogleBookItem(
    val volumeInfo: GoogleBookVolumeInfo? = null,
    val saleInfo: GoogleBookSaleInfo? = null
)

data class GoogleBookVolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val description: String? = null,
    val pageCount: Int? = null
)

data class GoogleBookSaleInfo(
    val listPrice: GoogleBookPrice? = null
)

data class GoogleBookPrice(
    val amount: Double? = null,
    val currencyCode: String? = null
)

data class AddStockRequest(
    val sku: String,
    val quantity: Int
)

data class UpdateStockRequest(
    val sku: String,
    val quantity: Int,
    val storeId: Long? = null
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

data class UserUpdateDTO(
    val password: String? = null,
    val storeIds: List<Long>? = null,
    val role: String? = null
)

data class CreateOrderRequest(
    val customerName: String?,
    val customerPhone: String?,
    val storeId: Long?,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val sku: String,
    val quantity: Int,
    val excludedProductSkus: List<String> = emptyList()
)

// Order Response Models
data class CustomerDTO(
    val id: Long? = null,
    val name: String? = null,
    val phone: String? = null
)

data class OrderLineDTO(
    val id: Long? = null,
    val productSku: String? = null,
    val productName: String? = null,
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    @SerializedName("isExclusion")
    val isExclusion: Boolean = false,
    val bundle: BundleDTO? = null
)

data class OrderDTO(
    val id: Long,
    val store: Store? = null,
    val processedBy: AppUser? = null,
    val customer: CustomerDTO? = null,
    val totalAmount: Double = 0.0,
    val discount: Double = 0.0,
    val status: String? = "COMPLETED",
    val reconciled: Boolean = false,
    val createdAt: String? = null,
    val orderLines: List<OrderLineDTO> = emptyList()
)

// Store Allocation & Return Models
data class AllocationItemDTO(
    val sku: String,
    val quantity: Int
)

data class AllocationRequestDTO(
    val items: List<AllocationItemDTO>
)

// Reconciliation Models
data class ReconciliationSummaryItem(
    val sku: String,
    val productName: String,
    val initialStock: Int = 0,
    val allocatedStock: Int = 0,
    val returnedStock: Int = 0,
    val soldQuantity: Int = 0,
    val remainingStock: Int = 0,
    val unitPrice: Double = 0.0,
    val totalRevenue: Double = 0.0
)

data class ReconciliationReportDTO(
    val storeId: Long? = null,
    val storeName: String? = null,
    val totalOrders: Int = 0,
    val totalGrossRevenue: Double = 0.0,
    val itemsSummary: List<ReconciliationSummaryItem> = emptyList(),
    val timestamp: String? = null
)

data class ReconciliationLogDTO(
    val id: Long,
    val store: Store? = null,
    val totalSalesValue: Double = 0.0,
    val itemsReturned: Boolean = false,
    val reportJson: String? = null,
    val reconciledAt: String? = null
)
