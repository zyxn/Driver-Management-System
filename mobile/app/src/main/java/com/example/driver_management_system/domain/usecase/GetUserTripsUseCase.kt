package com.example.driver_management_system.domain.usecase

import com.example.driver_management_system.domain.model.TripHistory
import com.example.driver_management_system.domain.repository.ReportRepository
import org.osmdroid.util.GeoPoint

data class UserTripsData(
    val trips: List<TripHistory>,
    val locationHistory: List<GeoPoint>
)

class GetUserTripsUseCase(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(userId: Int, date: String?): Result<UserTripsData> {
        return try {
            // Validate userId
            if (userId <= 0) {
                return Result.failure(Exception("Invalid user ID"))
            }
            
            // Fetch reports
            val reportsResult = reportRepository.getUserReports(userId, date)
            
            if (reportsResult.isFailure) {
                return Result.failure(reportsResult.exceptionOrNull() ?: Exception("Failed to fetch reports"))
            }
            
            val trips = reportsResult.getOrNull() ?: emptyList()
            
            // Fetch location history
            val locationResult = reportRepository.getLocationHistory(userId, 0, date)
            val locationHistory = locationResult.getOrNull() ?: emptyList()
            
            Result.success(UserTripsData(trips, locationHistory))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
