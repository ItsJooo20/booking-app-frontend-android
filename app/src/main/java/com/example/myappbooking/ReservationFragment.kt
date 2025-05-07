package com.example.myappbooking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.databinding.FragmentReservationBinding
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

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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

        setupDateTimeSelectors()
        setupDropdowns()
        setupSubmitButton()
        fetchCategories()
    }

    private fun setupDateTimeSelectors() {
        // Start Date
        binding.startDateLayout.setOnClickListener {
            showDatePicker { year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                binding.startDateText.text = dateFormat.format(calendar.time)
            }
        }

        // Start Time
        binding.startTimeLayout.setOnClickListener {
            showTimePicker { hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                binding.startTimeText.text = timeFormat.format(calendar.time)
            }
        }

        // End Date
        binding.endDateLayout.setOnClickListener {
            showDatePicker { year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                binding.endDateText.text = dateFormat.format(calendar.time)
            }
        }

        // End Time
        binding.endTimeLayout.setOnClickListener {
            showTimePicker { hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                binding.endTimeText.text = timeFormat.format(calendar.time)
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

    private fun setupDropdowns() {
        // Set up listeners for dropdown selection
        binding.categoryDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < categories.size) {
                    selectedCategoryId = categories[position].id
                    fetchFacilities(selectedCategoryId!!)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = null
            }
        }

        binding.facilityDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < facilities.size) {
                    selectedFacilityId = facilities[position].id
                    fetchFacilityItems(selectedFacilityId!!)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFacilityId = null
            }
        }

        binding.facilityItemsDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < facilityItems.size) {
                    selectedFacilityItemId = facilityItems[position].id
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedFacilityItemId = null
            }
        }
    }

    private fun setupSubmitButton() {
        binding.requestButton.setOnClickListener {
            if (validateInputs()) {
                createBooking()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Check if category is selected
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if facility is selected
        if (selectedFacilityId == null) {
            Toast.makeText(requireContext(), "Please select a facility", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if facility item is selected
        if (selectedFacilityItemId == null) {
            Toast.makeText(requireContext(), "Please select a facility item", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if start date and time are selected
        if (binding.startDateText.text.isNullOrEmpty() || binding.startTimeText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select start date and time", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if end date and time are selected
        if (binding.endDateText.text.isNullOrEmpty() || binding.endTimeText.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select end date and time", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if purpose is filled
        if (binding.purposeEditText.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a purpose for booking", Toast.LENGTH_SHORT).show()
            binding.purposeEditText.error = "Purpose is required"
            isValid = false
        }

        return isValid
    }

    private fun fetchCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getCategories(authHeader)

                    if (response.isSuccessful && response.body() != null) {
                        categories.clear()
                        categories.addAll(response.body()!!.data)

                        // Set up adapter for category dropdown
                        val categoryAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            categories.map { it.name }
                        )
                        binding.categoryDropdown.adapter = categoryAdapter
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch categories", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchFacilities(categoryId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getFacilities(authHeader, categoryId)

                    if (response.isSuccessful && response.body() != null) {
                        facilities.clear()
                        facilities.addAll(response.body()!!.data)

                        // Set up adapter for facility dropdown
                        val facilityAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            facilities.map { it.name }
                        )
                        binding.facilityDropdown.adapter = facilityAdapter

                        // Clear facility items since facility has changed
                        facilityItems.clear()
                        val emptyAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            emptyList<String>()
                        )
                        binding.facilityItemsDropdown.adapter = emptyAdapter
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch facilities", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchFacilityItems(facilityId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val prefMan = SharedPreferencesManager.getInstance(requireContext())
                val token = prefMan.getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getFacilityItems(authHeader, facilityId)

                    if (response.isSuccessful && response.body() != null) {
                        facilityItems.clear()
                        facilityItems.addAll(response.body()!!.data)

                        // Set up adapter for facility items dropdown
                        val facilityItemsAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            facilityItems.map { it.item_code }
                        )
                        binding.facilityItemsDropdown.adapter = facilityItemsAdapter
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch facility items", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun createBooking() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
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

//                    ApiClient.authService.createBooking(authHeader, bookingRequest)
                    handleBookingResponse(response)
//                    Toast.makeText(requireContext(), "Success submit request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                    Log.e("BOOKING_ERROR", "Exception saat submit booking", e)
                    Toast.makeText(requireContext(), "Kayaknya masuk sini deh coba yaa", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
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
            // Clear fields or navigate back
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
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
