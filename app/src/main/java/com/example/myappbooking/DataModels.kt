package com.example.myappbooking

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val phone: String?,
//    val photo: String?,
//    val access_token: String?,
)

data class LoginData(
    val user: User,
    val access_token: String?
)

data class EmailRequest(
    val email: String
)

data class VerificationResponse(
    val message: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val phone: String?,
    val role: String = "user"
)

data class RegisterResponse(
    val status: Boolean,
    val message: String,
    val data: RegisterData? = null,
//    val errors: ValidationErrors? = null,
    val error: String? = null
)

data class RegisterData(
    val user_id: Int,
    val email: String,
    val name: String
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

data class EmailApp(
    val name: String,
    val packageName: String,
    val activityName: String
)