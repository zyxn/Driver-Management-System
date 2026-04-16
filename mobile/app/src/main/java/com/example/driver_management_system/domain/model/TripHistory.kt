package com.example.driver_management_system.domain.model

data class TripHistory(
    val id: String,
    val type: TripType,
    val location: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val date: String, // Format: "2024-01-15"
    val timestamp: String,
    val notes: String,
    val photoUrl: String? = null,
    val signature: String? = null
)

enum class TripType(val displayName: String) {
    PICKUP("Pickup Barang"),
    DELIVERY("Pengiriman"),
    REST("Istirahat"),
    REFUEL("Isi Bahan Bakar"),
    CHECKPOINT("Checkpoint")
}
