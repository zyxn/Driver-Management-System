package com.example.driver_management_system.utils

import kotlin.math.*

object LocationUtils {
    /**
     * Calculate distance between two coordinates using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Format distance to human-readable string
     * @param distanceKm Distance in kilometers
     * @return Formatted string (e.g., "1.5 km", "500 m")
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 0.1 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 1.0 -> "${String.format("%.0f", distanceKm * 1000)} m"
            distanceKm < 10.0 -> "${String.format("%.1f", distanceKm)} km"
            else -> "${String.format("%.0f", distanceKm)} km"
        }
    }
}
