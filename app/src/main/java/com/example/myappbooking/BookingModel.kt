package com.example.myappbooking

import java.text.SimpleDateFormat
import java.util.Locale

// Model for the entire API response
data class BookingsResponse(
    val status: Boolean,
    val data: List<BookingData>
)

data class BookingResponse(
    val status: Boolean,
    val data: BookingData
)

// Model for each booking in the data array
data class BookingData(
    val id: Int,
    val start_datetime: String,
    val end_datetime: String,
    val status: String,
    val facility_item: FacilityItem,
    val user: User
) {
    // Computed property to directly access item_code from facility_item
    val item_code: String
        get() = facility_item.item_code

    // Computed property to directly access user's name
    val userName: String
        get() = user.name
}

data class BookingRequest(
    val facility_item_id: Int?,
    val start_datetime: String,
    val end_datetime: String,
    val purpose: String
)

data class BookingHistoryResponse(
    val status: String,
    val data: List<BookingHistoryItem>
)

data class BookingHistoryItem(
    val id: Int,
//    val facility_name: String,
    val item_code: String,
    val item_notes: String?,
    val start_datetime: String,
    val end_datetime: String,
    val purpose: String,
    val status: String,
    val created_at: String
) {
    // You can keep these helper functions or remove them since formatting is now done in ViewHolder
    fun getFormattedStartDate(): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return try {
            val date = inputFormat.parse(start_datetime)
            outputFormat.format(date)
        } catch (e: Exception) {
            start_datetime.split(" ")[0] // Fallback to just the date part
        }
    }

    fun getFormattedTimeRange(): String {
        val startTime = start_datetime.split(" ")[1].substring(0, 5)
        val endTime = end_datetime.split(" ")[1].substring(0, 5)
        return "$startTime - $endTime"
    }
}

// Nested model for facility item
