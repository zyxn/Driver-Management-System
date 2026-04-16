package com.example.driver_management_system.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeocodingService {
    @GET("data/reverse-geocode-client")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("localityLanguage") language: String = "id"
    ): Response<BigDataCloudResponse>
    
    companion object {
        fun create(): GeocodingService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.bigdatacloud.net/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(GeocodingService::class.java)
        }
    }
}

data class BigDataCloudResponse(
    val latitude: Double?,
    val longitude: Double?,
    val locality: String?,           // Kota/Kabupaten
    val localityInfo: LocalityInfo?,
    val city: String?,               // Kota
    val principalSubdivision: String?, // Provinsi
    val countryName: String?,
    val localityLanguageRequested: String?
)

data class LocalityInfo(
    val administrative: List<Administrative>?,
    val informative: List<Informative>?
)

data class Administrative(
    val name: String?,
    val description: String?,
    val order: Int?
)

data class Informative(
    val name: String?,
    val description: String?,
    val order: Int?
)
