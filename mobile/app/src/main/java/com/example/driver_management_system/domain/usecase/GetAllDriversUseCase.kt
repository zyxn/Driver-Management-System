package com.example.driver_management_system.domain.usecase

import com.example.driver_management_system.domain.model.Driver
import com.example.driver_management_system.domain.repository.DriverRepository

class GetAllDriversUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): Result<List<Driver>> {
        return driverRepository.getAllDrivers()
    }
}
