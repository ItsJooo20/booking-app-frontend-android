package com.example.myappbooking

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

// Nested model for facility item
