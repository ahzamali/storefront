package com.storefront.app.network

import com.storefront.app.model.*
import retrofit2.http.*

interface ApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any>

    // User Management
    @GET("/api/v1/auth/users")
    suspend fun getUsers(@Header("Authorization") token: String): List<AppUser>

    @POST("/api/v1/auth/register")
    suspend fun register(
        @Header("Authorization") token: String, 
        @Body request: CreateUserRequest
    ): AppUser

    @PUT("/api/v1/auth/users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: UserUpdateDTO
    ): AppUser

    @DELETE("/api/v1/auth/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String, 
        @Path("id") id: Long
    )

    // Stores
    @GET("/api/v1/stores")
    suspend fun getStores(@Header("Authorization") token: String): List<Store>

    @POST("/api/v1/stores")
    suspend fun createStore(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Store

    @POST("/api/v1/stores/{id}/allocate")
    suspend fun allocateStock(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: AllocationRequestDTO
    ): Any

    @POST("/api/v1/stores/{id}/return")
    suspend fun returnStock(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: AllocationRequestDTO
    ): Any

    @POST("/api/v1/stores/{id}/reconcile")
    suspend fun reconcileStore(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Query("returnStock") returnStock: Boolean = false
    ): ReconciliationReportDTO

    @GET("/api/v1/stores/{id}/reconciliations")
    suspend fun getReconciliationHistory(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): List<ReconciliationLogDTO>

    // Inventory
    @GET("/api/v1/inventory/view")
    suspend fun getInventoryView(
        @Header("Authorization") token: String, 
        @Query("storeId") storeId: Long? = null
    ): List<ProductStockDTO>
    
    @GET("/api/v1/inventory/bundles")
    suspend fun getBundles(@Header("Authorization") token: String): List<ProductStockDTO>

    @POST("/api/v1/inventory/products")
    suspend fun createProduct(
        @Header("Authorization") token: String,
        @Body request: CreateProductRequest
    ): Any

    @PUT("/api/v1/inventory/products/{id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: CreateProductRequest
    ): Any

    @DELETE("/api/v1/inventory/products/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    )

    @POST("/api/v1/inventory/stock")
    suspend fun addStock(
        @Header("Authorization") token: String,
        @Body request: AddStockRequest
    ): Any

    @POST("/api/v1/inventory/bundles")
    suspend fun createBundle(
        @Header("Authorization") token: String,
        @Body request: BundleDTO
    ): Any

    @POST("/api/v1/inventory/ingest/isbn")
    suspend fun ingestIsbn(
        @Header("Authorization") token: String,
        @Body payload: Map<String, Any>
    ): Any

    // Orders
    @POST("/api/v1/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String, 
        @Body orderRequest: CreateOrderRequest
    ): OrderDTO

    @GET("/api/v1/orders")
    suspend fun getOrders(
        @Header("Authorization") token: String,
        @Query("customerName") customerName: String? = null,
        @Query("customerPhone") customerPhone: String? = null,
        @Query("storeId") storeId: Long? = null
    ): List<OrderDTO>

    @GET("/api/v1/orders/reconciliation")
    suspend fun getReconciliationReport(
        @Header("Authorization") token: String,
        @Query("storeId") storeId: Long? = null
    ): Any
}
