package com.example.driver_management_system.domain.usecase

import android.location.Location
import com.example.driver_management_system.domain.repository.LocationRepository

class SendLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        userId: Int,
        location: Location
    ): Result<Boolean> {
        return locationRepository.sendLocationUpdate(userId, location)
    }
}
