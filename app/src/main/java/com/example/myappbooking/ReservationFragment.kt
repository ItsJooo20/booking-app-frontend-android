package com.example.myappbooking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.databinding.FragmentReservationBinding
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservationFragment : Fragment() {

    private var _binding: FragmentReservationBinding? = null
    private val binding get() = _binding!!

    private val categories = mutableListOf<FacilityCategory>()
    private val facilities = mutableListOf<Facility>()
    private val facilityItems = mutableListOf<FacilityItem>()

    private var selectedCategoryId: Int? = null
    private var selectedFacilityId: Int? = null
    private var selectedFacilityItemId: Int? = null

    // Save positions for UI restoration
    private var selectedCategoryPosition: Int = -1
    private var selectedFacilityPosition: Int = -1
    private var selectedFacilityItemPosition: Int = -1

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Variables to save datetime selections
    private var startDate: String? = null
    private var startTime: String? = null
    private var endDate: String? = null
    private var endTime: String? = null
    private var purpose: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore saved instance state if available
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }

        setupDateTimeSelectors()
        setupPowerSpinners()
        setupSubmitButton()
        fetchCategories()

        // Restore purpose text if available
        purpose?.let {
            binding.purposeEditText.setText(it)
        }

        // Restore date/time selections if available
        restoreDateTimeSelections()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save current selections
        outState.putInt("selectedCategoryPosition", selectedCategoryPosition)
        outState.putInt("selectedFacilityPosition", selectedFacilityPosition)
        outState.putInt("selectedFacilityItemPosition", selectedFacilityItemPosition)

        outState.putInt("selectedCategoryId", selectedCategoryId ?: -1)
        outState.putInt("selectedFacilityId", selectedFacilityId ?: -1)
        outState.putInt("selectedFacilityItemId", selectedFacilityItemId ?: -1)

        // Save datetime selections
        outState.putString("startDate", binding.startDateText.text?.toString())
        outState.putString("startTime", binding.startTimeText.text?.toString())
        outState.putString("endDate", binding.endDateText.text?.toString())
        outState.putString("endTime", binding.endTimeText.text?.toString())
        outState.putString("purpose", binding.purposeEditText.text?.toString())
    }

    private fun restoreState(savedInstanceState: Bundle) {
        selectedCategoryPosition = savedInstanceState.getInt("selectedCategoryPosition", -1)
        selectedFacilityPosition = savedInstanceState.getInt("selectedFacilityPosition", -1)
        selectedFacilityItemPosition = savedInstanceState.getInt("selectedFacilityItemPosition", -1)

        val categoryId = savedInstanceState.getInt("selectedCategoryId", -1)
        selectedCategoryId = if (categoryId != -1) categoryId else null

        val facilityId = savedInstanceState.getInt("selectedFacilityId", -1)
        selectedFacilityId = if (facilityId != -1) facilityId else null

        val facilityItemId = savedInstanceState.getInt("selectedFacilityItemId", -1)
        selectedFacilityItemId = if (facilityItemId != -1) facilityItemId else null

        // Restore datetime selections
        startDate = savedInstanceState.getString("startDate")
        startTime = savedInstanceState.getString("startTime")
        endDate = savedInstanceState.getString("endDate")
        endTime = savedInstanceState.getString("endTime")
        purpose = savedInstanceState.getString("purpose")
    }

    private fun restoreDateTimeSelections() {
        startDate?.let { binding.startDateText.text = it }
        startTime?.let { binding.startTimeText.text = it }
        endDate?.let { binding.endDateText.text = it }
        endTime?.let { binding.endTimeText.text = it }
    }

    private fun setupDateTimeSelectors() {
        // Start Date
        binding.startDateLayout.setOnClickListener {
            showDatePicker { year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                binding.startDateText.text = dateFormat.format(calendar.time)
                startDate = binding.startDateText.text.toString()
            }
        }

        // Start Time
        binding.startTimeLayout.setOnClickListener {
            showTimePicker { hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                binding.startTimeText.text = timeFormat.format(calendar.time)
                startTime = binding.startTimeText.text.toString()
            }
        }

        // End Date
        binding.endDateLayout.setOnClickListener {
            showDatePicker { year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                binding.endDateText.text = dateFormat.format(calendar.time)
                endDate = binding.endDateText.text.toString()
            }
        }

        // End Time
        binding.endTimeLayout.setOnClickListener {
            showTimePicker { hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                binding.endTimeText.text = timeFormat.format(calendar.time)
                endTime = binding.endTimeText.text.toString()
            }
        }
    }

    private fun showDatePicker(onDateSet: (year: Int, month: Int, day: Int) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                onDateSet(selectedYear, selectedMonth, selectedDay)
            },
            year, month, day
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showTimePicker(onTimeSet: (hourOfDay: Int, minute: Int) -> Unit) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, selectedMinute ->
                onTimeSet(hourOfDay, selectedMinute)
            },
            hour, minute, true
        ).show()
    }

    private fun setupPowerSpinners() {
        // Set up PowerSpinnerView
        with(binding.categoryDropdown) {
            viewLifecycleOwner.lifecycleScope.launch {
                setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newItem ->
                    if (newIndex >= 0 && newIndex < categories.size) {
                        selectedCategoryId = categories[newIndex].id
                        selectedCategoryPosition = newIndex
                        fetchFacilities(selectedCategoryId!!)
                        // Clear other spinners
                        binding.facilityDropdown.clearSelectedItem()
                        binding.facilityItemsDropdown.clearSelectedItem()
                        // Reset positions for other spinners
                        selectedFacilityPosition = -1
                        selectedFacilityItemPosition = -1
                        selectedFacilityId = null
                        selectedFacilityItemId = null
                    }
                }
            }
        }

        with(binding.facilityDropdown) {
            viewLifecycleOwner.lifecycleScope.launch {
                setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newItem ->
                    if (newIndex >= 0 && newIndex < facilities.size) {
                        selectedFacilityId = facilities[newIndex].id
                        selectedFacilityPosition = newIndex
                        fetchFacilityItems(selectedFacilityId!!)
                        // Clear facility items spinner
                        binding.facilityItemsDropdown.clearSelectedItem()
                        // Reset position for facility items spinner
                        selectedFacilityItemPosition = -1
                        selectedFacilityItemId = null
                    }
                }
            }
        }

        with(binding.facilityItemsDropdown) {
            viewLifecycleOwner.lifecycleScope.launch {
                setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newItem ->
                    if (newIndex >= 0 && newIndex < facilityItems.size) {
                        selectedFacilityItemId = facilityItems[newIndex].id
                        selectedFacilityItemPosition = newIndex
                    }
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.requestButton.setOnClickListener {
            // Save purpose text before validation
            purpose = binding.purposeEditText.text.toString()

            if (validateInputs()) {
                createBooking()
            }
        }
    }

    private fun validateInputs(): Boolean {
        // Check if category is selected
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if facility is selected
        if (selectedFacilityId == null) {
            Toast.makeText(requireContext(), "Please select a facility", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if facility item is selected
        if (selectedFacilityItemId == null) {
            Toast.makeText(requireContext(), "Please select a facility item", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if start date and time are selected
        if (binding.startDateText.text.isNullOrEmpty() || binding.startTimeText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select start date and time", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if end date and time are selected
        if (binding.endDateText.text.isNullOrEmpty() || binding.endTimeText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select end date and time", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if purpose is filled
        if (binding.purposeEditText.text.toString().trim().isEmpty()) {
            binding.purposeEditText.error = "Purpose is required"
            return false
        }

        return true
    }



    private fun fetchCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getCategories(authHeader)

                    if (response.isSuccessful && response.body() != null) {
                        categories.clear()
                        categories.addAll(response.body()!!.data)

                        // Set up PowerSpinnerView for category
                        val categoryNames = categories.map { it.name }
                        binding.categoryDropdown.setItems(categoryNames)

                        // If we have a previously selected category, select it again
                        if (selectedCategoryPosition >= 0 && selectedCategoryPosition < categoryNames.size) {
                            binding.categoryDropdown.selectItemByIndex(selectedCategoryPosition)
                            // The selection listener will handle fetching facilities
                        } else if (selectedCategoryId != null) {
                            // If we have the ID but not position, find the position
                            val position = categories.indexOfFirst { it.id == selectedCategoryId }
                            if (position >= 0) {
                                binding.categoryDropdown.selectItemByIndex(position)
                                selectedCategoryPosition = position
                            } else {
                                // If category with saved ID not found, fetch facilities manually
                                fetchFacilities(selectedCategoryId!!)
                            }
                        }
                    } else {
//                        Log.e("FETCH_ERROR", "Failed to fetch categories")
                    }
                }
            } catch (e: Exception) {
//                Log.e("FETCH_ERROR", "Error fetching categories: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fetchFacilities(categoryId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getFacilities(authHeader, categoryId)

                    if (response.isSuccessful && response.body() != null) {
                        facilities.clear()
                        facilities.addAll(response.body()!!.data)

                        // Set up PowerSpinnerView for facility
                        val facilityNames = facilities.map { it.name }
                        binding.facilityDropdown.setItems(facilityNames)

                        // If we have a previously selected facility, select it again
                        if (selectedFacilityPosition >= 0 && selectedFacilityPosition < facilityNames.size) {
                            binding.facilityDropdown.selectItemByIndex(selectedFacilityPosition)
                            // The selection listener will handle fetching facility items
                        } else if (selectedFacilityId != null) {
                            // If we have the ID but not position, find the position
                            val position = facilities.indexOfFirst { it.id == selectedFacilityId }
                            if (position >= 0) {
                                binding.facilityDropdown.selectItemByIndex(position)
                                selectedFacilityPosition = position
                            } else {
                                // If facility with saved ID not found, fetch facility items manually
                                fetchFacilityItems(selectedFacilityId!!)
                            }
                        } else {
                            // Clear facility items spinner if no facility is selected
                            binding.facilityItemsDropdown.clearSelectedItem()
                            binding.facilityItemsDropdown.setItems(emptyList<String>())
                        }
                    } else {
//                        Log.e("FETCH_ERROR", "Failed to fetch facilities")
                    }
                }
            } catch (e: Exception) {
//                Log.e("FETCH_ERROR", "Error fetching facilities: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fetchFacilityItems(facilityId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getFacilityItems(authHeader, facilityId)

                    if (response.isSuccessful && response.body() != null) {
                        facilityItems.clear()
                        facilityItems.addAll(response.body()!!.data)

                        // Set up PowerSpinnerView for facility items
                        val facilityItemCodes = facilityItems.map { it.item_code }
                        binding.facilityItemsDropdown.setItems(facilityItemCodes)

                        // If we have a previously selected facility item, select it again
                        if (selectedFacilityItemPosition >= 0 && selectedFacilityItemPosition < facilityItemCodes.size) {
                            binding.facilityItemsDropdown.selectItemByIndex(selectedFacilityItemPosition)
                        } else if (selectedFacilityItemId != null) {
                            // If we have the ID but not position, find the position
                            val position = facilityItems.indexOfFirst { it.id == selectedFacilityItemId }
                            if (position >= 0) {
                                binding.facilityItemsDropdown.selectItemByIndex(position)
                                selectedFacilityItemPosition = position
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
//                Log.e("FETCH_ERROR", "Error fetching facility items: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun createBooking() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                binding.requestButton.isEnabled = false

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    // Create start datetime
                    val startDate = binding.startDateText.text.toString()
                    val startTime = binding.startTimeText.text.toString()
                    val startCalendar = parseDateTime(startDate, startTime)
                    val startDatetime = apiDateTimeFormat.format(startCalendar.time)

                    // Create end datetime
                    val endDate = binding.endDateText.text.toString()
                    val endTime = binding.endTimeText.text.toString()
                    val endCalendar = parseDateTime(endDate, endTime)
                    val endDatetime = apiDateTimeFormat.format(endCalendar.time)

                    val bookingRequest = BookingRequest(
                        facility_item_id = selectedFacilityItemId,
                        start_datetime = startDatetime,
                        end_datetime = endDatetime,
                        purpose = binding.purposeEditText.text.toString().trim()
                    )

                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.createBooking(
                        authHeader,
                        bookingRequest
                    )

                    handleBookingResponse(response)
                }
            } catch (e: Exception) {
                Log.e("BOOKING_ERROR", "Exception during booking submission", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
                binding.requestButton.isEnabled = true
            }
        }
    }

    private fun parseDateTime(date: String, time: String): Calendar {
        val cal = Calendar.getInstance()

        // Parse date
        val dateParts = date.split(".")
        if (dateParts.size == 3) {
            cal.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
            cal.set(Calendar.MONTH, dateParts[1].toInt() - 1) // Month is 0-based
            cal.set(Calendar.YEAR, dateParts[2].toInt())
        }

        // Parse time
        val timeParts = time.split(":")
        if (timeParts.size == 2) {
            cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            cal.set(Calendar.MINUTE, timeParts[1].toInt())
            cal.set(Calendar.SECOND, 0)
        }

        return cal
    }

    private fun handleBookingResponse(response: Response<BookingResponse>) {
        if (response.isSuccessful && response.body() != null) {
            Toast.makeText(requireContext(), "Booking request submitted successfully!", Toast.LENGTH_LONG).show()
            // Clear all selections after successful submission
            clearAllSelections()
            // Pop back to previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = if (!errorBody.isNullOrEmpty()) {
                try {
                    JSONObject(errorBody).getString("message")
                } catch (e: Exception) {
                    "Failed to create booking"
                }
            } else {
                "Failed to create booking"
            }
            Toast.makeText(requireContext(), "End date and time must be after start date and time!", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearAllSelections() {
        // Clear selected IDs
        selectedCategoryId = null
        selectedFacilityId = null
        selectedFacilityItemId = null

        // Clear selected positions
        selectedCategoryPosition = -1
        selectedFacilityPosition = -1
        selectedFacilityItemPosition = -1

        // Clear UI selections
        binding.categoryDropdown.clearSelectedItem()
        binding.facilityDropdown.clearSelectedItem()
        binding.facilityItemsDropdown.clearSelectedItem()

        // Clear date/time selections
        binding.startDateText.text = ""
        binding.startTimeText.text = ""
        binding.endDateText.text = ""
        binding.endTimeText.text = ""

        // Clear purpose
        binding.purposeEditText.setText("")

        // Clear saved values
        startDate = null
        startTime = null
        endDate = null
        endTime = null
        purpose = null
    }

    private fun showLoading(show: Boolean) {
        // Show/hide overlay and disable/enable UI
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.requestButton.isEnabled = !show
        binding.ScrollView.isEnabled = !show

        // Disable bottom navigation in parent activity
        (activity as? MainActivity)?.setBottomNavEnabled(!show)
    }

    override fun onPause() {
        super.onPause()
        dismissAllSpinners()
    }

    override fun onStop() {
        super.onStop()
        dismissAllSpinners()
    }

    override fun onDestroyView() {
        dismissAllSpinners()
        _binding = null
        super.onDestroyView()
    }

    private fun dismissAllSpinners() {
        binding.categoryDropdown.dismiss()
        binding.facilityDropdown.dismiss()
        binding.facilityItemsDropdown.dismiss()
    }
}