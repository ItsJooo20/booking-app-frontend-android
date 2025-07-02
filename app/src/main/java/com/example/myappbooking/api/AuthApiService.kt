package com.example.myappbooking.api

import com.example.myappbooking.data.BaseResponse
import com.example.myappbooking.data.BookingDetailResponse
import com.example.myappbooking.data.BookingHistoryResponse
import com.example.myappbooking.data.BookingRequest
import com.example.myappbooking.data.BookingResponse
import com.example.myappbooking.data.BookingsResponse
import com.example.myappbooking.data.CategoryResponse
import com.example.myappbooking.data.ChangePasswordRequest
import com.example.myappbooking.data.ChangePhoneRequest
import com.example.myappbooking.data.EmailRequest
import com.example.myappbooking.data.EquipmentReturnResponse
import com.example.myappbooking.data.FacilityItemsDetailResponse
import com.example.myappbooking.data.FacilityItemsResponse
import com.example.myappbooking.data.FacilityResponse
import com.example.myappbooking.data.LoginRequest
import com.example.myappbooking.data.LoginResponse
import com.example.myappbooking.data.LogoutResponse
import com.example.myappbooking.data.RegisterRequest
import com.example.myappbooking.data.RegisterResponse
import com.example.myappbooking.data.VerificationResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
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

    @Multipart
    @POST("bookings/{booking}/return")
    suspend fun submitReturn(
        @Header("Authorization") token: String,
        @Path("booking") bookingId: Int,
        @Part returnPhoto: MultipartBody.Part,
        @Part("user_condition") userCondition: RequestBody,
        @Part("notes") notes: RequestBody
    ): Response<EquipmentReturnResponse>

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Header("Authorization") token: String,
        @Path("id") bookingId: Int
    ): Response<BaseResponse>

    @GET("facilities/items/{id}")
    suspend fun getFacilityItemDetail(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int
    ): Response<FacilityItemsDetailResponse>

    @GET("bookings/{id}")
    suspend fun getBookingDetails(
        @Header("Authorization") token: String,
        @Path("id") bookingId: Int
    ): Response<BookingDetailResponse>

    @POST("user/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body requestBody: ChangePasswordRequest
    ): Response<BaseResponse>

    @POST("user/change-phone")
    suspend fun changePhoneNumber(
        @Header("Authorization") token: String,
        @Body requestBody: ChangePhoneRequest
    ): Response<BaseResponse>

    @Multipart
    @POST("user/profile-image")
    suspend fun updateProfileImage(
        @Header("Authorization") token: String,
        @Part profile_image: MultipartBody.Part
    ): Response<BaseResponse>

    @DELETE("user/profile-image")
    suspend fun removeProfileImage(
        @Header("Authorization") token: String
    ): Response<BaseResponse>
}