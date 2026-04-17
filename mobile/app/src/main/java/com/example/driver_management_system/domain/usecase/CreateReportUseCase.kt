package com.example.driver_management_system.domain.usecase

import android.net.Uri
import com.example.driver_management_system.domain.repository.ReportRepository

class CreateReportUseCase(private val repository: ReportRepository) {
    suspend operator fun invoke(
        userId: Int,
        placeName: String,
        description: String,
        latitude: Double,
        longitude: Double,
        imageUri: Uri
    ): Result<Boolean> {
        return repository.createReport(
            userId = userId,
            placeName = placeName,
            description = description,
            latitude = latitude,
            longitude = longitude,
            imageUri = imageUri
        )
    }
}
