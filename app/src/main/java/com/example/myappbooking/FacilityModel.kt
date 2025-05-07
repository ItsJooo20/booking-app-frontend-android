package com.example.myappbooking

data class FacilityCategory(
    val id: Int,
    val name: String,
//    val path_img: String?, // Added path_img field for icon image
    val facilities_count: Int
)

data class CategoryResponse(
    val status: Boolean,
    val data: List<FacilityCategory>
)
data class Facility(
    val id: Int,
    val name: String,
    val description: String?,
    val category_id: Int,
    val items_count: Int,
    val category: FacilityCategory?
)

data class FacilityResponse(
    val status: Boolean,
    val data: List<Facility>
)

data class FacilityItemsResponse(
    val status: Boolean,
    val data: List<FacilityItem>
)

data class FacilityItem(
    val id: Int,
    val item_code: String,
    val notes: String,
    val facility: Facility?
)