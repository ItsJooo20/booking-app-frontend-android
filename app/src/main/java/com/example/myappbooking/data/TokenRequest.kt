package com.example.myappbooking.data

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("device_token")
    val deviceToken: String,

    @SerializedName("device_type")
    val deviceType: String = "android"
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)