package com.example.driver_management_system.domain.repository

import com.example.driver_management_system.domain.model.TripHistory
import org.osmdroid.util.GeoPoint

interface ReportRepository {
    suspend fun getUserReports(userId: Int, date: String?): Result<List<TripHistory>>
    suspend fun getLocationHistory(userId: Int, limit: Int, date: String?): Result<List<GeoPoint>>
}
