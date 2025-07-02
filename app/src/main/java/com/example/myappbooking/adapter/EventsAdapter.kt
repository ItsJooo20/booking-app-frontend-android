package com.example.myappbooking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myappbooking.R
import com.example.myappbooking.data.BookingData
import com.example.myappbooking.utility.ImageUtils
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter(private val events: List<BookingData>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
    }

    override fun getItemCount() = events.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvStartDateTime: TextView = itemView.findViewById(R.id.tvStartDateTime)
        private val tvFinishDateTime: TextView = itemView.findViewById(R.id.tvFinishDateTime)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivRoomImage)

        fun bind(event: BookingData) {
            tvEventTitle.text = event.item_code
            tvStatus.text = "Booked"
            tvStartDateTime.text = formatDateTime(event.start_datetime)
            tvFinishDateTime.text = formatDateTime(event.end_datetime)

            ImageUtils.loadImage(
                context = ivImage.context,
                imagePath = event.facility_item.facility_item_image!!.image_path,
                imageView = ivImage
            )
        }
    }

    private fun formatDateTime(dateTimeString: String): String {
        return try {
            // Try to parse with the format from API: "2025-08-30 06:00:00"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            val outputFormat = SimpleDateFormat("dd MMMM yy - hh:mm a", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            // If that fails, try other formats as fallback
            try {
                // Try format: "16-June-2025, 14:30"
                val inputFormat = SimpleDateFormat("d-MMMM-yyyy, HH:mm", Locale.ENGLISH)
                val date = inputFormat.parse(dateTimeString)
                val outputFormat = SimpleDateFormat("dd MMMM yy - hh:mm a", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e2: Exception) {
                try {
                    // Try alternative format without full month name
                    val inputFormat = SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(dateTimeString)
                    val outputFormat = SimpleDateFormat("dd MMM - hh:mm a", Locale.getDefault())
                    outputFormat.format(date!!)
                } catch (e3: Exception) {
                    dateTimeString // Final fallback to original string
                }
            }
        }
    }
}