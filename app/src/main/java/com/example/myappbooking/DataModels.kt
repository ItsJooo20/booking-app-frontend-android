package com.example.myappbooking

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val phone: String?,
//    val photo: String?,
    val remember_token: String?,
)

data class LoginData(
    val user: User,
    val remember_token: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val data: LoginData
)

data class LogoutResponse(
    val status: Boolean,
    val message: String
)