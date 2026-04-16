package com.example.driver_management_system.data.repository

import android.content.Context
import com.example.driver_management_system.data.remote.RetrofitClient
import com.example.driver_management_system.domain.model.TripHistory
import com.example.driver_management_system.domain.model.TripType
import com.example.driver_management_system.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

class ReportRepositoryImpl(private val context: Context) : ReportRepository {
    
    private val apiService by lazy { RetrofitClient.getAuthenticatedApiService(context) }
    
    override suspend fun getUserReports(userId: Int, date: String?): Result<List<TripHistory>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserReports(userId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val reports = response.body()?.data ?: emptyList()
                    val trips = reports.map { dto ->
                        val tripType = determineTripType(dto.title, dto.description)
                        
                        // Parse timestamp
                        val timestamp = try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val parsedDate = dateFormat.parse(dto.reportedAtLocal ?: dto.createdAt)
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            "${timeFormat.format(parsedDate ?: Date())} WIB"
                        } catch (e: Exception) {
                            "00:00 WIB"
                        }
                        
                        // Extract date
                        val dateStr = try {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val parsedDate = dateFormat.parse(dto.reportedAtLocal ?: dto.createdAt)
                            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            outputFormat.format(parsedDate ?: Date())
                        } catch (e: Exception) {
                            getCurrentDate()
                        }
                        
                        TripHistory(
                            id = dto.id.toString(),
                            type = tripType,
                            location = dto.placeName ?: dto.title,
                            address = "Lat: ${dto.latitude}, Lng: ${dto.longitude}",
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            date = dateStr,
                            timestamp = timestamp,
                            notes = dto.description,
                            photoUrl = dto.imageUrl,
                            signature = if (dto.status == "completed") "signed" else null
                        )
                    }
                    Result.success(trips)
                } else {
                    Result.failure(Exception("Failed to fetch reports: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getLocationHistory(userId: Int, limit: Int, date: String?): Result<List<GeoPoint>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ReportRepository", "Fetching location history for userId: $userId, date: $date, limit: $limit")
                
                val response = apiService.getLocationHistory(userId, limit, date)
                
                android.util.Log.d("ReportRepository", "Location history response: ${response.code()}, success: ${response.isSuccessful}")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val locations = response.body()?.data ?: emptyList()
                    android.util.Log.d("ReportRepository", "Location history count: ${locations.size}")
                    
                    val geoPoints = locations.map { GeoPoint(it.latitude, it.longitude) }
                    Result.success(geoPoints)
                } else {
                    val errorMsg = "Failed to fetch location history: ${response.message()}"
                    android.util.Log.e("ReportRepository", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportRepository", "Exception fetching location history", e)
                Result.failure(e)
            }
        }
    }
    
    private fun determineTripType(title: String, description: String): TripType {
        val text = "$title $description".lowercase()
        
        return when {
            text.contains("pickup") || text.contains("ambil") || text.contains("muat") -> TripType.PICKUP
            text.contains("delivery") || text.contains("kirim") || text.contains("antar") -> TripType.DELIVERY
            text.contains("refuel") || text.contains("isi") || text.contains("bensin") || text.contains("solar") -> TripType.REFUEL
            text.contains("rest") || text.contains("istirahat") || text.contains("break") -> TripType.REST
            text.contains("checkpoint") || text.contains("check") -> TripType.CHECKPOINT
            else -> TripType.CHECKPOINT // Default
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
