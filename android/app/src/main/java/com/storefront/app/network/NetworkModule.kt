package com.storefront.app.network

import com.storefront.app.ConfigManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    
    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createApiService(baseUrl: String): ApiService {
        return createRetrofit(baseUrl).create(ApiService::class.java)
    }
}
