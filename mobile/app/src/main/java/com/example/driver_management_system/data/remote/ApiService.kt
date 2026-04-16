package com.example.driver_management_system.data.remote

import com.example.driver_management_system.data.remote.dto.LoginRequest
import com.example.driver_management_system.data.remote.dto.LoginResponse
import com.example.driver_management_system.data.remote.dto.ReportResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("locations/track")
    suspend fun trackLocation(@Body request: TrackLocationRequest): Response<TrackLocationResponse>
    
    @GET("reports/user/{userId}")
    suspend fun getUserReports(
        @Path("userId") userId: Int,
        @Query("date") date: String? = null
    ): Response<ReportResponse>
    
    @GET("locations/user/{userId}")
    suspend fun getLocationHistory(
        @Path("userId") userId: Int,
        @Query("limit") limit: Int = 0,
        @Query("date") date: String? = null
    ): Response<LocationHistoryResponse>
    
    @GET("users/drivers/with-location")
    suspend fun getAllDriversWithLocation(): Response<com.example.driver_management_system.data.remote.dto.DriverListResponse>
    
    @POST("users/fcm-token")
    suspend fun updateFCMToken(@Body request: UpdateFCMTokenRequest): Response<UpdateFCMTokenResponse>
}

data class LocationHistoryResponse(
    val success: Boolean,
    val data: List<LocationHistoryDto>?
)

data class LocationHistoryDto(
    val id: Int,
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val speed: Double?,
    val bearing: Double?,
    val altitude: Double?,
    val timestamp: String,
    val created_at: String
)

data class TrackLocationRequest(
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Double,
    val heading: Double?,
    val altitude: Double?,
    val recorded_at_utc: String,
    val timezone: String,
    val recorded_at_local: String
)

data class TrackLocationResponse(
    val success: Boolean,
    val data: LocationData?
)

data class LocationData(
    val id: Int,
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Double,
    val heading: Double?,
    val altitude: Double?,
    val recorded_at_utc: String,
    val timezone: String,
    val recorded_at_local: String,
    val created_at: String
)

data class UpdateFCMTokenRequest(
    val fcm_token: String
)

data class UpdateFCMTokenResponse(
    val success: Boolean,
    val message: String
)
