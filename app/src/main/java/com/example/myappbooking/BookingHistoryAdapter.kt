package com.example.myappbooking

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

class BookingHistoryAdapter(
    private val onFilteredCountChanged: ((Int) -> Unit)? = null,
    private val onReturnClicked: ((BookingHistoryItem) -> Unit)? = null
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
        private val btnReturn: MaterialButton = itemView.findViewById(R.id.btnReturn)

        fun bind(booking: BookingHistoryItem) {
            tvEventTitle.text = "${booking.item_code}"

            // Set status
            tvStatus.text = booking.status.replaceFirstChar { it.uppercase() }
                .replace("_", " ") // Format "return_submitted" to "Return submitted"

            when (booking.status.lowercase()) {
                "approved" -> {
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                "pending" -> {
                    tvStatus.setTextColor(Color.parseColor("#FFC107")) // Yellow
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                "rejected" -> {
                    tvStatus.setTextColor(Color.parseColor("#F44336")) // Red
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                "completed" -> {
                    tvStatus.setTextColor(Color.parseColor("#2196F3")) // Blue
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                "cancelled" -> {
                    tvStatus.setTextColor(Color.parseColor("#9E9E9E")) // Gray
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                "needs return" -> {
                    tvStatus.setTextColor(Color.parseColor("#FF9800")) // Orange
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.VISIBLE
                    btnReturn.setOnClickListener {
                        onReturnClicked?.invoke(booking)
                    }
                }
                "return_submitted" -> {
                    tvStatus.setTextColor(Color.parseColor("#9C27B0")) // Purple for return submitted
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
                }
                else -> {
                    tvStatus.setTextColor(Color.parseColor("#757575")) // Default gray
                    statusCard.setCardBackgroundColor(Color.WHITE)
                    btnReturn.visibility = View.GONE
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
            .inflate(R.layout.item_event_with_return, parent, false)
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
        // Get today's date in system timezone
        val today = LocalDate.now()

        return when (timeFilter.lowercase()) {
            "today" -> list.filter {
                try {
                    val bookingDate = parseDateFromBackendFormat(it.start_datetime)
                    bookingDate?.isEqual(today) == true
                } catch (e: Exception) {
                    Log.e("BookingHistoryAdapter", "Error parsing date for today filter: ${e.message}")
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
                            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                        } ?: false
                    } catch (e: Exception) {
                        Log.e("BookingHistoryAdapter", "Error parsing date for week filter: ${e.message}")
                        false
                    }
                }
            }
            "month" -> {
                val currentMonth = today.monthValue
                val currentYear = today.year
                list.filter {
                    try {
                        val bookingDate = parseDateFromBackendFormat(it.start_datetime)
                        bookingDate?.let { date ->
                            date.monthValue == currentMonth && date.year == currentYear
                        } ?: false
                    } catch (e: Exception) {
                        Log.e("BookingHistoryAdapter", "Error parsing date for month filter: ${e.message}")
                        false
                    }
                }
            }
            else -> list
        }
    }

    private fun parseDateFromBackendFormat(dateTimeString: String): LocalDate? {
        // Extract date part from datetime string
        val datePart = dateTimeString.split(",").firstOrNull()?.trim() ?: return null

        // Try multiple date formats
        val formats = listOf(
            DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
        )

        for (formatter in formats) {
            try {
                return LocalDate.parse(datePart, formatter)
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }

        // If all parsing attempts fail, log and return null
        Log.e("BookingHistoryAdapter", "Failed to parse date: $datePart")
        return null
    }
}