package com.example.myappbooking.utility

import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val createdDateFormat = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm a", Locale.getDefault())

    fun formatApiDateToDisplayDate(apiDate: String): String {
        return try {
            val date = apiDateFormat.parse(apiDate)
            if (date != null) displayDateFormat.format(date) else apiDate
        } catch (e: Exception) {
            apiDate
        }
    }

    fun formatApiDateToDisplayTime(apiDate: String): String {
        return try {
            val date = apiDateFormat.parse(apiDate)
            if (date != null) displayTimeFormat.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun formatApiDateToCreatedDate(apiDate: String): String {
        return try {
            val date = apiDateFormat.parse(apiDate)
            if (date != null) createdDateFormat.format(date) else apiDate
        } catch (e: Exception) {
            apiDate
        }
    }
}