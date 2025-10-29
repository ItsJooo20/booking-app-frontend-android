package com.example.myappbooking.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.BookingRequest
import com.example.myappbooking.data.BookingResponse
import com.example.myappbooking.databinding.ActivityReservationByItemBinding
import com.example.myappbooking.utility.NetworkUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservationByItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationByItemBinding
    private var itemID: Int = 0
    private var itemCODE: String = ""

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var startDate: String? = null
    private var startTime: String? = null
    private var endDate: String? = null
    private var endTime: String? = null
    private var purpose: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationByItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NetworkUtils.init(this)

        setupItem()
        setupViews()
        setupDateTimeSelectors()
        setupSubmitButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
    }

    private fun setupDateTimeSelectors() {
        // Start Date
        binding.startDateLayout.setOnClickListener {
            showMaterialDatePicker { selectedDate ->
                calendar.timeInMillis = selectedDate
                binding.startDateText.text = dateFormat.format(calendar.time)
                startDate = binding.startDateText.text.toString()
            }
        }

        // Start Time
        binding.startTimeLayout.setOnClickListener {
            showMaterialTimePicker { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                binding.startTimeText.text = timeFormat.format(calendar.time)
                startTime = binding.startTimeText.text.toString()
            }
        }

        // End Date
        binding.endDateLayout.setOnClickListener {
            showMaterialDatePicker { selectedDate ->
                calendar.timeInMillis = selectedDate
                binding.endDateText.text = dateFormat.format(calendar.time)
                endDate = binding.endDateText.text.toString()
            }
        }

        // End Time
        binding.endTimeLayout.setOnClickListener {
            showMaterialTimePicker { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                binding.endTimeText.text = timeFormat.format(calendar.time)
                endTime = binding.endTimeText.text.toString()
            }
        }
    }

    private fun showMaterialDatePicker(onDateSelected: (selectedDate: Long) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(System.currentTimeMillis())
                    .build()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            onDateSelected(selection)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showMaterialTimePicker(onTimeSelected: (hour: Int, minute: Int) -> Unit) {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            onTimeSelected(timePicker.hour, timePicker.minute)
        }

        timePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun setupSubmitButton() {
        binding.requestButton.setOnClickListener {
            purpose = binding.purposeEditText.text.toString()

            if (validateInputs()) {
                createBooking()
            }
        }
    }

    private fun createBooking() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                binding.requestButton.isEnabled = false

                val prefMan = SharedPreferencesManager.getInstance(this@ReservationByItemActivity)
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
                        facility_item_id = itemID,
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
                Toast.makeText(this@ReservationByItemActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
                binding.requestButton.isEnabled = true
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.requestButton.isEnabled = !show
//        binding.ScrollView.isEnabled = !show

    }

    private fun handleBookingResponse(response: Response<BookingResponse>) {
        if (response.isSuccessful && response.body() != null) {
            val bookingData = response.body()!!.data
            val bookingId = bookingData.id
//            Toast.makeText(this, "Booking request submitted successfully!", Toast.LENGTH_LONG).show()
            clearAllSelections()
            navigateToSuccess(bookingId)
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
            Toast.makeText(this, "End date and time must be after start date and time!", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearAllSelections() {
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

    private fun validateInputs(): Boolean {
        if (binding.startDateText.text.isNullOrEmpty() || binding.startTimeText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select start date and time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.endDateText.text.isNullOrEmpty() || binding.endTimeText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select end date and time", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.purposeEditText.text.toString().trim().isEmpty()) {
            binding.purposeEditText.error = "Purpose is required"
            return false
        }

        return true
    }

    private fun navigateToSuccess(bookingId: Int) {
        val intent = Intent(this, SuccessSentActivity::class.java)
        intent.putExtra("BOOKING_ID", bookingId)
        intent.putExtra("ITEM_CODE", itemCODE)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupItem() {
        itemID = intent.getIntExtra("ITEM_ID", 0)
        itemCODE = intent.getStringExtra("ITEM_CODE")?: ""
    }

    private fun setupViews() {
        binding.tvCategoryTitle.text = itemCODE
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}