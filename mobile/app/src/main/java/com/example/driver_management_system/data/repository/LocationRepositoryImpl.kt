package com.example.driver_management_system.data.repository

import android.location.Location
import android.util.Log
import com.example.driver_management_system.data.remote.ApiService
import com.example.driver_management_system.data.remote.TrackLocationRequest
import com.example.driver_management_system.domain.repository.LocationRepository
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LocationRepositoryImpl(
    private val apiService: ApiService
) : LocationRepository {

    override suspend fun sendLocationUpdate(
        userId: Int,
        location: Location
    ): Result<Boolean> {
        return try {
            // Convert timestamp to UTC and Local time in RFC3339 format
            val timestamp = location.time
            val instant = Instant.ofEpochMilli(timestamp)
            
            // UTC time
            val utcFormatter = DateTimeFormatter.ISO_INSTANT
            val recordedAtUtc = utcFormatter.format(instant)
            
            // Local time with timezone
            val localZoneId = ZoneId.systemDefault()
            val localDateTime = instant.atZone(localZoneId)
            val localFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val recordedAtLocal = localFormatter.format(localDateTime)
            
            // Timezone name
            val timezone = localZoneId.id
            
            // Convert speed from m/s to km/h
            val speedKmh = location.speed * 3.6
            
            val request = TrackLocationRequest(
                user_id = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                speed = speedKmh.toDouble(),
                accuracy = location.accuracy.toDouble(),
                heading = if (location.hasBearing()) location.bearing.toDouble() else null,
                altitude = if (location.hasAltitude()) location.altitude else null,
                recorded_at_utc = recordedAtUtc,
                timezone = timezone,
                recorded_at_local = recordedAtLocal
            )
            
            Log.d("LocationRepository", "Sending location: $request")
            
            val response = apiService.trackLocation(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("LocationRepository", "Location sent successfully: ${response.body()?.data}")
                Result.success(true)
            } else {
                val errorMsg = "Failed to send location: ${response.code()} - ${response.message()}"
                Log.e("LocationRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "Error sending location", e)
            Result.failure(e)
        }
    }
}
