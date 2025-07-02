package com.example.myappbooking.data

data class FacilityCategory(
    val id: Int,
    val name: String,
    val image_path: String?,
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
    val category: FacilityCategory?,
    val image_path: String?
)

data class FacilityResponse(
    val status: Boolean,
    val data: List<Facility>
)

data class FacilityItemsResponse(
    val status: Boolean,
    val data: List<FacilityItem>
)

data class FacilityItemsDetailResponse(
    val status: Boolean,
    val data: FacilityItem
)

data class FacilityItem(
    val id: Int,
    val item_code: String,
    val notes: String?,
    val facility: Facility?,
    val images: List<Images>?,
    val primary_image_url: String?,
    val facility_item_image: FacilityItemImage?
)

data class FacilityItemImage(
    val id: Int,
    val facility_item_id: Int,
    val image_path: String,
    val is_primary: Int
)

data class Images(
    val id: Int,
    val is_primary: Int,
    val image_path: String?,
)