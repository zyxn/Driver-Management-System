package com.example.driver_management_system.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val licenseNo: String?,
    val role: String,
    val status: String
)
