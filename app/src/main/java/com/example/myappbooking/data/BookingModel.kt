package com.example.myappbooking.data

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
    val item_code: String
        get() = facility_item.item_code
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
    val booking_id: Int,
//    val facility_name: String,
    val item_code: String,
    val image_path: String?,
    val start_datetime: String,
    val end_datetime: String,
    val purpose: String,
    val status: String,
    val created_at: String
)

data class EquipmentReturnResponse(
    val message: String,
    val data: EquipmentReturn?
)

data class EquipmentReturn(
    val id: Int,
    val booking_id: Int,
    val return_date: String,
    val return_photo_path: String,
    val user_condition: String,
    val condition_status: String,
    val notes: String?,
    val created_at: String,
    val updated_at: String
)

data class BookingDetailResponse(
    val status: Boolean,
    val message: String,
    val data: BookingDetail
)

data class BookingDetail(
    val id: Int,
    val user_id: Int,
    val facility_item_id: Int,
    val start_datetime: String,
    val end_datetime: String,
    val purpose: String,
    val status: String,
    val created_at: String,
    val updated_at: String,
    val user: User?,
    val facility_item: FacilityItem
)

data class BaseResponse(
    val status: Boolean,
    val message: String,
    val data: Any? = null
)
