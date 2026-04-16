package com.example.driver_management_system.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: LoginData?
)

data class LoginData(
    @SerializedName("user")
    val user: UserDto
)

data class UserDto(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("full_name")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("license_no")
    val licenseNo: String?,
    
    @SerializedName("role")
    val role: String,
    
    @SerializedName("status")
    val status: String
)
