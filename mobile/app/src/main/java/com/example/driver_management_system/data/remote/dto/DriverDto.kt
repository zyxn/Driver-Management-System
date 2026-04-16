package com.example.driver_management_system.data.remote.dto

data class DriverListResponse(
    val success: Boolean,
    val data: List<DriverDto>?
)

data class DriverDto(
    val id: Int,
    val name: String?,
    val email: String?,
    val phone: String?,
    val role: String?,
    val last_location: LastLocationDto?
)

data class LastLocationDto(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val speed: Double?,
    val timestamp: String
)
