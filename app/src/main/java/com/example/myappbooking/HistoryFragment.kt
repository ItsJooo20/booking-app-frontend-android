package com.example.myappbooking

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myappbooking.databinding.FragmentHistoryBinding
import com.example.myappbooking.databinding.FragmentProfileBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch


class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var adapter: BookingHistoryAdapter
    private lateinit var authApiService: AuthApiService
    private var currentStatusFilter: String? = null
    private var currentTimeFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authApiService = ApiClient.authService
        setupRecyclerView()
        setupChips()
        loadBookingHistory()
    }

    private fun setupRecyclerView() {
        adapter = BookingHistoryAdapter { filteredCount ->
            // Callback from adapter to update empty state
            updateEmptyState(filteredCount)
        }
        binding.rvEvents.adapter = adapter
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupChips() {
        // Status chips
        binding.chipPending.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "pending"
                clearOtherStatusChips(binding.chipPending)
                applyFilters()
            } else if (currentStatusFilter == "pending") {
                currentStatusFilter = null
                applyFilters()
            }
        }

        binding.chipApproved.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "approved"
                clearOtherStatusChips(binding.chipApproved)
                applyFilters()
            } else if (currentStatusFilter == "approved") {
                currentStatusFilter = null
                applyFilters()
            }
        }

        binding.chipRejected.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "rejected"
                clearOtherStatusChips(binding.chipRejected)
                applyFilters()
            } else if (currentStatusFilter == "rejected") {
                currentStatusFilter = null
                applyFilters()
            }
        }

        binding.chipCompleted.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "completed"
                clearOtherStatusChips(binding.chipCompleted)
                applyFilters()
            } else if (currentStatusFilter == "completed") {
                currentStatusFilter = null
                applyFilters()
            }
        }

        binding.chipCancelled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "cancelled"
                clearOtherStatusChips(binding.chipCancelled)
                applyFilters()
            } else if (currentStatusFilter == "cancelled") {
                currentStatusFilter = null
                applyFilters()
            }
        }

        // Time filter chips
        binding.chipToday.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentTimeFilter = "today"
                clearOtherTimeChips(binding.chipToday)
                applyFilters()
            } else if (currentTimeFilter == "today") {
                currentTimeFilter = null
                applyFilters()
            }
        }

        binding.chipWeek.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentTimeFilter = "week"
                clearOtherTimeChips(binding.chipWeek)
                applyFilters()
            } else if (currentTimeFilter == "week") {
                currentTimeFilter = null
                applyFilters()
            }
        }

        binding.chipMonth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentTimeFilter = "month"
                clearOtherTimeChips(binding.chipMonth)
                applyFilters()
            } else if (currentTimeFilter == "month") {
                currentTimeFilter = null
                applyFilters()
            }
        }
    }

    private fun clearOtherStatusChips(selectedChip: Chip) {
        listOf(
            binding.chipPending,
            binding.chipApproved,
            binding.chipRejected,
            binding.chipCompleted,
            binding.chipCancelled
        ).forEach { chip ->
            if (chip != selectedChip) {
                chip.isChecked = false
            }
        }
    }

    private fun clearOtherTimeChips(selectedChip: Chip) {
        listOf(
            binding.chipToday,
            binding.chipWeek,
            binding.chipMonth
        ).forEach { chip ->
            if (chip != selectedChip) {
                chip.isChecked = false
            }
        }
    }

    private fun applyFilters() {
        // Apply both filters together to ensure they work in combination
        adapter.applyBothFilters(currentStatusFilter, currentTimeFilter)
    }

    private fun loadBookingHistory() {
        lifecycleScope.launch {
            try {
                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                val authHeader = "Bearer $token"
                val response = authApiService.getBookingHistory(
                    authHeader,
                    null, // Don't filter on server side, we'll filter locally
                    null  // Don't filter on server side, we'll filter locally
                )

                if (response.isSuccessful) {
                    val bookings = response.body()?.data ?: emptyList()
                    adapter.submitList(bookings)

                    // Update empty state based on original data
                    updateEmptyState(bookings.size)
                } else {
                    showError("Failed to load booking history")
                    updateEmptyState(0)
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
                updateEmptyState(0)
            }
        }
    }

    private fun updateEmptyState(itemCount: Int) {
        if (itemCount == 0) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        binding.rvEvents.visibility = View.GONE
        binding.notFoundCard.visibility = View.VISIBLE

        // Update empty state message based on active filters
        val hasFilters = currentStatusFilter != null || currentTimeFilter != null
        if (hasFilters) {
            binding.tvError.text = "No Results Found"
            binding.tvErrorLoad.text = "No events match your current filter criteria. Try adjusting your filters to see more results."
        } else {
            binding.tvError.text = "No History Found"
            binding.tvErrorLoad.text = "You haven't made any bookings yet. Start booking facilities to see your history here."
        }
    }

    private fun hideEmptyState() {
        binding.rvEvents.visibility = View.VISIBLE
        binding.notFoundCard.visibility = View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}