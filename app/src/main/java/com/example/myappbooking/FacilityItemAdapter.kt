package com.example.myappbooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myappbooking.databinding.ItemFacilityBinding

class FacilityItemAdapter(
    facilityItemsList: List<FacilityItem>,
    private val onItemClick: (FacilityItem) -> Unit
) : RecyclerView.Adapter<FacilityItemAdapter.ItemViewHolder>() {

    private var originalList: List<FacilityItem> = facilityItemsList
    private var filteredList: List<FacilityItem> = facilityItemsList

    inner class ItemViewHolder(private val binding: ItemFacilityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(facilityItem: FacilityItem) {
            binding.apply {
                // Bind facility item data to views
                // Assuming your item_facility.xml has these views:
                tvFacilityName.text = facilityItem.item_code
                tvFacilityDescription.text = facilityItem.notes

                // You can also show facility name if needed
                facilityItem.facility?.let { facility ->
                    tvFacilityCategory.text = facility.name
                    tvFacilityCategory.visibility = View.VISIBLE
                } ?: run {
                    tvFacilityCategory.visibility = View.GONE
                }

                // Handle click
                root.setOnClickListener {
                    onItemClick(facilityItem)
                }

                // You can add more binding here based on your item layout
                // For example, if you have an image view:
                // ivFacilityImage.setImageResource(R.drawable.ic_item_default)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemFacilityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newList: List<FacilityItem>) {
        originalList = newList
        filteredList = newList.toList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter { item ->
                item.item_code.contains(query, ignoreCase = true) ||
                        item.notes.contains(query, ignoreCase = true) ||
                        (item.facility?.name?.contains(query, ignoreCase = true) == true)
            }
        }
        notifyDataSetChanged()
    }
}