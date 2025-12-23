package com.storefront.app.network

import com.storefront.app.model.*
import retrofit2.http.*

interface ApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any> // token response

    // User Management
    @GET("/api/v1/auth/users")
    suspend fun getUsers(@Header("Authorization") token: String): List<AppUser>

    @POST("/api/v1/auth/register")
    suspend fun register(@Header("Authorization") token: String, @Body request: CreateUserRequest): AppUser

    @DELETE("/api/v1/auth/users/{id}")
    suspend fun deleteUser(@Header("Authorization") token: String, @Path("id") id: Long)

    // Stores
    @GET("/api/v1/stores")
    suspend fun getStores(@Header("Authorization") token: String): List<Store>

    // Inventory
    @GET("/api/v1/inventory/view")
    suspend fun getInventoryView(
        @Header("Authorization") token: String, 
        @Query("storeId") storeId: Long? = null
    ): List<ProductStockDTO>
    
    @GET("/api/v1/inventory/bundles")
    suspend fun getBundles(@Header("Authorization") token: String): List<ProductStockDTO> // Reuse DTO or separate

    // Orders
    @POST("/api/v1/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String, 
        @Body orderRequest: CreateOrderRequest
    ): Map<String, Any>
}
