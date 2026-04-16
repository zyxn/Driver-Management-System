package com.example.driver_management_system.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReportResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<ReportDto>?
)

data class ReportDto(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("place_name")
    val placeName: String?,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("image_url")
    val imageUrl: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("reported_at_local")
    val reportedAtLocal: String?,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
)
