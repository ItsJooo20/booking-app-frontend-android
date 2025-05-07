package com.example.myappbooking

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myappbooking.R
import com.example.myappbooking.databinding.ItemCategoryBinding

class CategoryAdapter(
    categoryList: List<FacilityCategory>,
    private val onItemClick: (FacilityCategory) -> Unit) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitleCategory: TextView = itemView.findViewById(R.id.category_title)
        //private val icCategory: ImageView = itemView.findViewById(R.id.category_icon)

        fun bind(category: FacilityCategory) {
            tvTitleCategory.text = category.name
            itemView.setOnClickListener{
                onItemClick(category)
            }
        }
    }

    // Initialize with the provided list
    private var originalList: List<FacilityCategory> = categoryList
    private var filteredList: List<FacilityCategory> = categoryList

    fun updateData(newList: List<FacilityCategory>) {
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
                it.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    // Use filteredList for size
    override fun getItemCount(): Int = filteredList.size

    // Use filteredList for binding
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }
}