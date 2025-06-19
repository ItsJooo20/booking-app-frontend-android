package com.example.myappbooking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

class BookingHistoryAdapter(
    private val onFilteredCountChanged: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<BookingHistoryAdapter.BookingViewHolder>() {

    private var bookings = mutableListOf<BookingHistoryItem>()
    private var originalBookings = mutableListOf<BookingHistoryItem>()
    private var currentStatusFilter: String? = null
    private var currentTimeFilter: String? = null

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivRoomImage: ImageView = itemView.findViewById(R.id.ivRoomImage)
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvStartDateTime: TextView = itemView.findViewById(R.id.tvStartDateTime)
        private val tvFinishDateTime: TextView = itemView.findViewById(R.id.tvFinishDateTime)
        private val statusCard: CardView = itemView.findViewById(R.id.cardStatus)

        fun bind(booking: BookingHistoryItem) {
            tvEventTitle.text = "${booking.item_code}"

            // Set status
            tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }
            when (booking.status.lowercase()) {
                "approved" -> {
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
                    statusCard.setCardBackgroundColor(Color.WHITE)
                }
                "pending" -> {
                    tvStatus.setTextColor(Color.parseColor("#FFC107")) // Yellow
                    statusCard.setCardBackgroundColor(Color.WHITE)
                }
                "rejected" -> {
                    tvStatus.setTextColor(Color.parseColor("#F44336")) // Red
                    statusCard.setCardBackgroundColor(Color.WHITE)
                }
                "completed" -> {
                    tvStatus.setTextColor(Color.parseColor("#2196F3")) // Blue
                    statusCard.setCardBackgroundColor(Color.WHITE)
                }
                "cancelled" -> {
                    tvStatus.setTextColor(Color.parseColor("#9E9E9E")) // Gray
                    statusCard.setCardBackgroundColor(Color.WHITE)
                }
            }

            // Set date/time - Handle the backend format
            tvStartDateTime.text = formatDateTime(booking.start_datetime)
            tvFinishDateTime.text = formatDateTime(booking.end_datetime)

            // You can set different icons based on facility type if needed
            ivRoomImage.setImageResource(R.drawable.pic_check)
        }

        private fun formatDateTime(dateTimeString: String): String {
            return try {
                // Backend format: "16-June-2025, 14:30"
                val inputFormat = SimpleDateFormat("d-MMMM-yyyy, HH:mm", Locale.ENGLISH)
                val date = inputFormat.parse(dateTimeString)
                val outputFormat = SimpleDateFormat("dd MMM - hh:mm a", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                // Fallback for other possible formats
                try {
                    // Try alternative format without full month name
                    val inputFormat = SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    val outputFormat = SimpleDateFormat("dd MMM - hh:mm a", Locale.getDefault())
                    outputFormat.format(date!!)
                } catch (e2: Exception) {
                    dateTimeString // Final fallback to original string
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun submitList(newList: List<BookingHistoryItem>) {
        bookings.clear()
        bookings.addAll(newList)
        originalBookings.clear()
        originalBookings.addAll(newList)
        notifyDataSetChanged()
    }

    fun filterByStatus(status: String?) {
        currentStatusFilter = status
        applyFilters()
    }

    fun filterByTime(timeFilter: String?) {
        currentTimeFilter = timeFilter
        applyFilters()
    }

    fun applyBothFilters(status: String?, timeFilter: String?) {
        currentStatusFilter = status
        currentTimeFilter = timeFilter
        applyFilters()
    }

    private fun applyFilters() {
        var filteredList = originalBookings.toList()

        // Apply status filter
        if (currentStatusFilter != null) {
            filteredList = filteredList.filter {
                it.status.equals(currentStatusFilter, ignoreCase = true)
            }
        }

        // Apply time filter
        if (currentTimeFilter != null) {
            filteredList = applyTimeFilter(filteredList, currentTimeFilter!!)
        }

        bookings.clear()
        bookings.addAll(filteredList)
        notifyDataSetChanged()

        // Notify fragment about filtered count
        onFilteredCountChanged?.invoke(filteredList.size)
    }

    private fun applyTimeFilter(list: List<BookingHistoryItem>, timeFilter: String): List<BookingHistoryItem> {
        val today = LocalDate.now()

        return when (timeFilter.lowercase()) {
            "today" -> list.filter {
                try {
                    val bookingDate = parseDateFromBackendFormat(it.start_datetime)
                    bookingDate?.isEqual(today) == true
                } catch (e: Exception) {
                    false
                }
            }
            "week" -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                list.filter {
                    try {
                        val bookingDate = parseDateFromBackendFormat(it.start_datetime)
                        bookingDate?.let { date ->
                            (date.isEqual(startOfWeek) || date.isAfter(startOfWeek)) &&
                                    (date.isEqual(endOfWeek) || date.isBefore(endOfWeek))
                        } == true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            "month" -> {
                list.filter {
                    try {
                        val bookingDate = parseDateFromBackendFormat(it.start_datetime)
                        bookingDate?.let { date ->
                            date.year == today.year && date.month == today.month
                        } == true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            else -> list
        }
    }

    private fun parseDateFromBackendFormat(dateTimeString: String): LocalDate? {
        return try {
            // Backend format: "16-June-2025, 14:30"
            val datePart = dateTimeString.split(",")[0].trim() // Get "16-June-2025"

            // Parse using DateTimeFormatter for LocalDate
            val formatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH)
            LocalDate.parse(datePart, formatter)
        } catch (e: Exception) {
            // Try alternative parsing methods
            try {
                // Try with SimpleDateFormat and convert to LocalDate
                val inputFormat = SimpleDateFormat("d-MMMM-yyyy", Locale.ENGLISH)
                val datePart = dateTimeString.split(",")[0].trim()
                val date = inputFormat.parse(datePart)

                // Convert java.util.Date to LocalDate
                val calendar = java.util.Calendar.getInstance()
                calendar.time = date!!
                LocalDate.of(
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )
            } catch (e2: Exception) {
                null
            }
        }
    }
}