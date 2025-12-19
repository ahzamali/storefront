package com.storefront.app.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any>

    @POST("/api/v1/orders")
    suspend fun createOrder(@Header("Authorization") token: String, @Body orderRequest: Map<String, Any>): Map<String, Any>

    @GET("/api/v1/inventory/products")
    suspend fun getProducts(@Header("Authorization") token: String): List<Map<String, Any>>

    @GET("/api/v1/stores")
    suspend fun getStores(@Header("Authorization") token: String): List<Map<String, Any>>
    
    // We can define DTOs data classes later if needed, but Maps are quick for now to avoid duplications across platforms
    // Ideally we should share data models or recreate them. I will assume we use Maps/native objects for simplicity unless complex.
    // Specially for Compose, Data classes are better.
}
