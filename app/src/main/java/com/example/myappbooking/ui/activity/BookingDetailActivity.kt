package com.example.myappbooking.ui.activity

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.BookingDetail
import com.example.myappbooking.databinding.ActivityBookingDetailBinding
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BookingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailBinding
    private var bookingId: Int = 0
    private var itemCode: String = ""

    // Date formatters
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val createdDateFormat = SimpleDateFormat("MMMM d, yyyy 'at' hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialData()
        setupListeners()
        fetchBookingDetails()
    }

    private fun setupInitialData() {
        bookingId = intent.getIntExtra("BOOKING_ID", 0)
        itemCode = intent.getStringExtra("ITEM_CODE") ?: ""

        // Set initial text while loading
        binding.tvBookingId.text = "Booking Code - $bookingId"
        binding.tvFacilityItem.text = itemCode
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnCopyBookingId.setOnClickListener {
            copyBookingIdToClipboard()
        }

        binding.btnCancelBooking.setOnClickListener {
            showCancelConfirmationDialog()
//            Toast.makeText(this, "Cancellation functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnContactSupport.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun copyBookingIdToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Booking ID", "Booking Code - $bookingId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Booking ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun fetchBookingDetails() {
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(this@BookingDetailActivity).getAuthToken()
                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getBookingDetails(authHeader, bookingId)

                    if (response.isSuccessful && response.body() != null) {
                        val bookingDetail = response.body()!!.data
                        updateUI(bookingDetail)
                    } else {
                        Toast.makeText(this@BookingDetailActivity,
                            "Failed to load booking details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingDetail", "Error fetching booking details", e)
                Toast.makeText(this@BookingDetailActivity,
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(bookingDetail: BookingDetail) {
        // Booking ID
        binding.tvBookingId.text = "Booking Code - ${bookingDetail.id}"

        // Booking Status
        binding.tvBookingStatus.text = bookingDetail.status.capitalize()
        updateStatusColor(bookingDetail.status)

        // Facility Information
        binding.tvFacilityItem.text = bookingDetail.facility_item.item_code

        // Load facility image if available - FIXED NULL HANDLING
        val facilityItemImage = bookingDetail.facility_item.facility_item_image
        if (facilityItemImage != null && !facilityItemImage.image_path.isNullOrEmpty()) {
            ImageUtils.loadImage(
                context = binding.imgFacility.context,
                imagePath = facilityItemImage.image_path,
                imageView = binding.imgFacility
            )
        } else {
            // Use a placeholder image when no image is available
            binding.imgFacility.setImageResource(R.drawable.pic_check)
        }

        // Schedule Information
        try {
            // Parse dates from the API response
            val startDateStr = bookingDetail.start_datetime
            val endDateStr = bookingDetail.end_datetime

            Log.d("BookingDetail", "Original start: $startDateStr, end: $endDateStr")

            // Fix format if necessary - if API returns different format than expected
            val startDateTime = parseApiDateTime(startDateStr)
            val endDateTime = parseApiDateTime(endDateStr)

            if (startDateTime != null) {
                binding.tvStartDate.text = displayDateFormat.format(startDateTime)
                binding.tvStartTime.text = displayTimeFormat.format(startDateTime)
                Log.d("BookingDetail", "Parsed start: ${displayDateFormat.format(startDateTime)} ${displayTimeFormat.format(startDateTime)}")
            } else {
                binding.tvStartDate.text = "Invalid date"
                binding.tvStartTime.text = "Invalid time"
            }

            if (endDateTime != null) {
                binding.tvEndDate.text = displayDateFormat.format(endDateTime)
                binding.tvEndTime.text = displayTimeFormat.format(endDateTime)
                Log.d("BookingDetail", "Parsed end: ${displayDateFormat.format(endDateTime)} ${displayTimeFormat.format(endDateTime)}")
            } else {
                binding.tvEndDate.text = "Invalid date"
                binding.tvEndTime.text = "Invalid time"
            }
        } catch (e: Exception) {
            Log.e("BookingDetail", "Error parsing dates", e)
            binding.tvStartDate.text = "Error parsing date"
            binding.tvStartTime.text = ""
            binding.tvEndDate.text = "Error parsing date"
            binding.tvEndTime.text = ""
        }

        // Purpose Information
        binding.tvPurpose.text = bookingDetail.purpose

        // Set the cancel button visibility based on status
        if (bookingDetail.status.lowercase() == "pending") {
            binding.btnCancelBooking.visibility = View.VISIBLE
        } else {
            binding.btnCancelBooking.visibility = View.GONE

            // Make the Home button take full width
            val params = binding.btnContactSupport.layoutParams as LinearLayout.LayoutParams
            params.width = LinearLayout.LayoutParams.MATCH_PARENT
            params.marginStart = 0
            binding.btnContactSupport.layoutParams = params
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Confirm") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun cancelBooking() {
        // Show loading overlay
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(this@BookingDetailActivity).getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.cancelBooking(authHeader, bookingId)

                    // Hide loading overlay
                    showLoading(false)

                    if (response.isSuccessful) {
                        Toast.makeText(this@BookingDetailActivity,
                            "Booking cancelled successfully", Toast.LENGTH_SHORT).show()

                        // Refresh booking details to update status
                        fetchBookingDetails()
                    } else {
                        // Handle error response
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                JSONObject(errorBody).getString("message")
                            } catch (e: Exception) {
                                "Failed to cancel booking"
                            }
                        } else {
                            "Failed to cancel booking"
                        }

                        Toast.makeText(this@BookingDetailActivity,
                            errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // Hide loading overlay
                showLoading(false)

                Log.e("BookingDetail", "Error cancelling booking", e)
                Toast.makeText(this@BookingDetailActivity,
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper method to show/hide the loading overlay
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    // This function handles multiple possible datetime formats from the API
    private fun parseApiDateTime(dateTimeStr: String): Date? {
        val possibleFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        )

        // Configure all formats to handle UTC time correctly
        possibleFormats.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }

        for (format in possibleFormats) {
            try {
                // Parse the date in UTC
                val utcDate = format.parse(dateTimeStr)

                // Convert UTC date to local time zone for display
                if (utcDate != null) {
                    return utcDate
                }
            } catch (e: Exception) {
                // Try the next format
                continue
            }
        }

        Log.e("BookingDetail", "Failed to parse date: $dateTimeStr")
        return null
    }

    private fun updateStatusColor(status: String) {
        when (status.lowercase()) {
            "pending" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_pending))
            }
            "approved" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_approved))
            }
            "rejected" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_rejected))
            }
            "completed" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.primary_dark_color))
            }
            "cancelled" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.text_gray_color))
            }
            "needs return" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.orange_color))
            }
            "return submitted" -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.purple_color))
            }
            else -> {
                binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.primary_color))
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}