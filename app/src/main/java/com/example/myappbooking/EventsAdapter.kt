package com.example.myappbooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

        fun bind(event: BookingData) {
            tvEventTitle.text = event.item_code
            tvStatus.text = "Booked"
            tvStartDateTime.text = event.start_datetime
            tvFinishDateTime.text = event.end_datetime
        }
    }
}