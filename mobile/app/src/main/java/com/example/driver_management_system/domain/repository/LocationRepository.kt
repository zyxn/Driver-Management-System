package com.example.driver_management_system.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun sendLocationUpdate(
        userId: Int,
        location: Location
    ): Result<Boolean>
}
