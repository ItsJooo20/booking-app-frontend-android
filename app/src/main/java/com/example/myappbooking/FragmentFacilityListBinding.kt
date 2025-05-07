package com.example.myappbooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FragmentFacilityListBinding(val root: View) {
    val toolbar: androidx.appcompat.widget.Toolbar = root.findViewById(R.id.toolbar)
    val btnBack: View = root.findViewById(R.id.btn_back)
    val tvCategoryTitle: TextView = root.findViewById(R.id.tv_category_title)
    val etSearch: android.widget.EditText = root.findViewById(R.id.et_search)
    val rvFacilities: RecyclerView = root.findViewById(R.id.rv_facilities)
    val progressBar: android.widget.ProgressBar = root.findViewById(R.id.progress_bar)
    val emptyState: android.widget.LinearLayout = root.findViewById(R.id.empty_state)

    companion object {
        fun inflate(inflater: LayoutInflater, container: ViewGroup?, attachToRoot: Boolean): FragmentFacilityListBinding {
            val view = inflater.inflate(R.layout.fragment_facility_list, container, attachToRoot)
            return FragmentFacilityListBinding(view)
        }
    }
}
