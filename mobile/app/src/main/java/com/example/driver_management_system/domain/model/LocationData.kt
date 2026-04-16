package com.example.driver_management_system.domain.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class TrackingState(
    val isTracking: Boolean = false,
    val currentLocation: LocationData? = null,
    val totalDistance: Float = 0f,
    val duration: Long = 0L
)
