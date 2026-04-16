package com.example.driver_management_system.data.remote

import android.content.Context
import android.util.Log
import com.example.driver_management_system.data.local.PreferencesManager
import com.example.driver_management_system.utils.AuthEvent
import com.example.driver_management_system.utils.AuthEventBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var authenticatedRetrofit: Retrofit? = null
    private var context: Context? = null
    
    fun init(appContext: Context) {
        context = appContext
    }
    
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            // Interceptor untuk bypass ngrok warning
            val ngrokInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .addHeader("User-Agent", "DriverManagementApp")
                    .build()
                chain.proceed(request)
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(ngrokInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()
            
            retrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    
    private fun getAuthenticatedRetrofit(ctx: Context): Retrofit {
        if (authenticatedRetrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            // Auth Interceptor - menambahkan token ke setiap request
            val authInterceptor = Interceptor { chain ->
                val preferencesManager = PreferencesManager(ctx)
                val token = runBlocking {
                    preferencesManager.token.first()
                }
                
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .addHeader("User-Agent", "DriverManagementApp")
                
                // Add token as cookie if available
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Cookie", "token=$token")
                }
                
                val response = chain.proceed(requestBuilder.build())
                
                // Handle 401 Unauthorized (token expired)
                if (response.code == 401) {
                    // Clear token on unauthorized
                    runBlocking {
                        preferencesManager.clearAll()
                        // Emit event to notify UI
                        AuthEventBus.emit(AuthEvent.TokenExpired)
                    }
                    Log.w("RetrofitClient", "Token expired or invalid, cleared token")
                }
                
                response
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build()
            
            authenticatedRetrofit = Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return authenticatedRetrofit!!
    }
    
    // For unauthenticated requests (login)
    val apiService: ApiService by lazy {
        getRetrofit().create(ApiService::class.java)
    }
    
    // For authenticated requests
    fun getAuthenticatedApiService(ctx: Context): ApiService {
        return getAuthenticatedRetrofit(ctx).create(ApiService::class.java)
    }
}

