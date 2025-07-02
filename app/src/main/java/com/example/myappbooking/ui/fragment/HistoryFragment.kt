package com.example.myappbooking.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myappbooking.R
import com.example.myappbooking.ui.activity.ReturnItemActivity
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.adapter.BookingHistoryAdapter
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.api.AuthApiService
import com.example.myappbooking.data.BookingHistoryItem
import com.example.myappbooking.databinding.FragmentHistoryBinding
import com.example.myappbooking.ui.activity.BookingDetailActivity
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException(
        "Attempt to access binding after onDestroyView() or before onCreateView()"
    )
    private lateinit var adapter: BookingHistoryAdapter
    private lateinit var authApiService: AuthApiService
    private var currentStatusFilter: String? = null
    private var currentTimeFilter: String? = null
    private var loadJob: Job? = null

    companion object {
        private const val RETURN_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.status_approved,
            R.color.status_pending
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadBookingHistory()
        }

        clearAllChip()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authApiService = ApiClient.authService
        setupRecyclerView()
        setupChips()
        loadBookingHistory()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Your custom back press handling here
            requireActivity().finishAffinity() // To exit the app
        }
    }

    private fun setupRecyclerView() {
        adapter = BookingHistoryAdapter(
            onFilteredCountChanged = { filteredCount ->
                updateEmptyState(filteredCount)
            },
            onReturnClicked = { booking ->
                openReturnActivity(booking)
            },
            onItemClicked = { booking ->
                openBookingDetailActivity(booking)
            }
        )
        binding.rvEvents.adapter = adapter
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun openBookingDetailActivity(booking: BookingHistoryItem) {
        val intent = Intent(requireContext(), BookingDetailActivity::class.java).apply {
            putExtra("BOOKING_ID", booking.booking_id)
            putExtra("ITEM_CODE", booking.item_code)
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupChips() {

        binding.chipReturnsubmiited.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "return submitted"
                clearOtherStatusChips(binding.chipReturnsubmiited)
                applyFilters()
            } else if (currentStatusFilter == "return submitted") {
                currentStatusFilter = null
                applyFilters()
            }
        }

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

        // Add needs return chip
        binding.chipNeedsreturn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentStatusFilter = "needs return"
                clearOtherStatusChips(binding.chipNeedsreturn)
                applyFilters()
            } else if (currentStatusFilter == "needs return") {
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
            binding.chipCancelled,
            binding.chipNeedsreturn,
            binding.chipReturnsubmiited,
        ).forEach { chip ->
            if (chip != selectedChip) {
                chip.isChecked = false
            }
        }
    }

    private fun clearAllChip() {
        _binding?.let{ binding ->
            listOf(
                binding.chipPending,
                binding.chipApproved,
                binding.chipRejected,
                binding.chipCompleted,
                binding.chipCancelled,
                binding.chipNeedsreturn,
                binding.chipReturnsubmiited,
                binding.chipToday,
                binding.chipWeek,
                binding.chipMonth
            ).forEach { chip ->
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

    private fun openReturnActivity(booking: BookingHistoryItem) {
        val intent = Intent(requireContext(), ReturnItemActivity::class.java).apply {
            putExtra(ReturnItemActivity.EXTRA_BOOKING_ID, booking.booking_id)
            putExtra(ReturnItemActivity.EXTRA_ITEM_CODE, booking.item_code)
        }
        startActivityForResult(intent, RETURN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RETURN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the booking history after successful return
            loadBookingHistory()
            showSuccess("Return submitted successfully!")
        }
    }

    private fun loadBookingHistory() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
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

                    binding.swipeRefreshLayout.isRefreshing = false
                    // Update empty state based on original data
                    updateEmptyState(bookings.size)
                } else {
                    binding.swipeRefreshLayout.isRefreshing = false
                    showError("Failed to load booking history")
                    updateEmptyState(0)
                }
            } catch (e: Exception) {
                if (isActive) { // Only handle errors if the coroutine is still active
                    binding.swipeRefreshLayout.isRefreshing = false
                    updateEmptyState(0)
                }
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

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        clearAllChip()
        super.onDestroyView()
        _binding = null
    }
}