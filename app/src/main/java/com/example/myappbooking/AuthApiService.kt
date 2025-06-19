package com.example.myappbooking

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("email/resend")
    suspend fun resendVerifEmail(@Body request: EmailRequest): Response<VerificationResponse>

    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(@Header("Authorization") token: String): Response<LogoutResponse>

    @GET("facilities/categories")
    suspend fun getCategories(@Header("Authorization") token: String): Response<CategoryResponse>

    @GET("approved-events")
    suspend fun getApprovedEvent(@Header("Authorization") token: String): Response<BookingsResponse>

    @GET("facilities")
    suspend fun getFacilities(
        @Header("Authorization") authHeader: String,
        @Query("category_id") categoryId: Int
    ): Response<FacilityResponse>

    @GET("facilities/items")
    suspend fun getFacilityItems(
        @Header("Authorization") authHeader: String,
        @Query("facility_id") facilityId: Int
    ): Response<FacilityItemsResponse>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body request: BookingRequest
    ): Response<BookingResponse>

    @GET("bookings/history")
    suspend fun getBookingHistory(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("time_filter") timeFilter: String? = null
    ): Response<BookingHistoryResponse>
}