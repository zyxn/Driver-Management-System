package com.example.driver_management_system.domain.repository

import com.example.driver_management_system.domain.model.Driver

interface DriverRepository {
    suspend fun getAllDrivers(): Result<List<Driver>>
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): Result<String>
}
