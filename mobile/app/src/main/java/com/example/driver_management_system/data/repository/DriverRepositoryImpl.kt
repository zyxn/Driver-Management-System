package com.example.driver_management_system.data.repository

import android.content.Context
import com.example.driver_management_system.data.remote.ApiService
import com.example.driver_management_system.data.remote.GeocodingService
import com.example.driver_management_system.domain.model.Driver
import com.example.driver_management_system.domain.model.DriverLocation
import com.example.driver_management_system.domain.repository.DriverRepository
import kotlinx.coroutines.delay

class DriverRepositoryImpl(
    private val apiService: ApiService,
    private val geocodingService: GeocodingService = GeocodingService.create()
) : DriverRepository {
    
    override suspend fun getAllDrivers(): Result<List<Driver>> {
        return try {
            val response = apiService.getAllDriversWithLocation()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val drivers = response.body()?.data?.mapNotNull { dto ->
                    // Skip if essential fields are null
                    if (dto.name.isNullOrEmpty() || dto.email.isNullOrEmpty()) {
                        return@mapNotNull null
                    }
                    
                    Driver(
                        id = dto.id,
                        name = dto.name,
                        email = dto.email,
                        phone = dto.phone,
                        role = dto.role ?: "driver",
                        lastLocation = dto.last_location?.let { loc ->
                            DriverLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                speed = loc.speed,
                                timestamp = loc.timestamp
                            )
                        }
                    )
                } ?: emptyList()
                
                Result.success(drivers)
            } else {
                Result.failure(Exception("Failed to fetch drivers: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): Result<String> {
        return try {
            // Small delay to avoid overwhelming the API
            delay(300)
            
            android.util.Log.d("DriverRepository", "Fetching address for: $latitude, $longitude")
            
            val response = geocodingService.reverseGeocode(latitude, longitude)
            
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d("DriverRepository", "BigDataCloud response: $body")
                
                val formattedAddress = buildString {
                    // Try to get street/road from informative data
                    body?.localityInfo?.informative?.firstOrNull { 
                        it.description?.contains("road", ignoreCase = true) == true ||
                        it.description?.contains("street", ignoreCase = true) == true
                    }?.name?.let { 
                        append(it)
                        android.util.Log.d("DriverRepository", "Road: $it")
                    }
                    
                    // Add locality (kelurahan/desa)
                    body?.locality?.let { 
                        if (isNotEmpty()) append(", ")
                        append(it)
                        android.util.Log.d("DriverRepository", "Locality: $it")
                    }
                    
                    // Add city
                    body?.city?.let { 
                        if (isNotEmpty()) append(", ")
                        append(it)
                        android.util.Log.d("DriverRepository", "City: $it")
                    }
                    
                    // Add province if city is empty
                    if (body?.city.isNullOrEmpty()) {
                        body?.principalSubdivision?.let { 
                            if (isNotEmpty()) append(", ")
                            append(it)
                            android.util.Log.d("DriverRepository", "Province: $it")
                        }
                    }
                }
                
                val finalAddress = when {
                    formattedAddress.isNotEmpty() -> formattedAddress
                    else -> "Alamat tidak tersedia"
                }
                
                android.util.Log.d("DriverRepository", "Final address: $finalAddress")
                Result.success(finalAddress)
            } else {
                android.util.Log.e("DriverRepository", "BigDataCloud error: ${response.code()} - ${response.message()}")
                Result.success("Alamat tidak tersedia")
            }
        } catch (e: Exception) {
            android.util.Log.e("DriverRepository", "Exception fetching address: ${e.message}", e)
            Result.success("Alamat tidak tersedia")
        }
    }
}
