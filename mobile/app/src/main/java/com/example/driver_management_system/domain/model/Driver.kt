package com.example.driver_management_system.domain.model

data class Driver(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,
    val lastLocation: DriverLocation?
)

data class DriverLocation(
    val latitude: Double,
    val longitude: Double,
    val speed: Double?,
    val timestamp: String,
    val address: String = ""
)
