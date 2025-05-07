package com.example.myappbooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myappbooking.databinding.ItemFacilityBinding


class FacilityAdapter(
    facilityList: List<Facility>,
    private val onItemClick: (Facility) -> Unit
) : RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder>() {

    inner class FacilityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFacilityName: TextView = itemView.findViewById(R.id.tv_facility_name)
        private val tvFacilityCategory: TextView = itemView.findViewById(R.id.tv_facility_category)
        private val tvFacilityDescription: TextView = itemView.findViewById(R.id.tv_facility_description)

        fun bind(facility: Facility) {
            tvFacilityName.text = facility.name
            tvFacilityCategory.text = facility.category?.name ?: ""
            tvFacilityDescription.text = facility.description

            itemView.setOnClickListener {
                onItemClick(facility)
            }
        }
    }

    // Initialize with the provided list
    private var originalList: List<Facility> = facilityList
    private var filteredList: List<Facility> = facilityList

    fun updateData(newList: List<Facility>) {
        originalList = newList
        filteredList = newList.toList()
        notifyDataSetChanged()
    }

    // Method to filter the data
    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacilityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_facility, parent, false)
        return FacilityViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: FacilityViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }
}


