package com.example.myappbooking.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.BookingDetail
import com.example.myappbooking.databinding.ActivityBookingDetailBinding
import com.example.myappbooking.ui.fragment.DashboardFragment
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.utility.NetworkUtils
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

    // Locale Indonesia untuk display
    private val localeID = Locale("id", "ID")

    // Formatter untuk tampilan (zona waktu lokal)
    private val displayDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", localeID).apply {
        timeZone = TimeZone.getDefault()
    }
    private val displayTimeFormat = SimpleDateFormat("HH:mm", localeID).apply {
        timeZone = TimeZone.getDefault()
    }
    private val createdDateFormat = SimpleDateFormat("d MMMM yyyy 'pukul' HH.mm", localeID).apply {
        timeZone = TimeZone.getDefault()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContactSupport.setOnClickListener {
            // Misal kembali ke Home
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("target_tab", R.id.nav_home)
            startActivity(intent)

//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
        }

        NetworkUtils.init(this)

        setupInitialData()
        setupListeners()
        fetchBookingDetails()
    }

    private fun setupInitialData() {
        bookingId = intent.getIntExtra("BOOKING_ID", 0)
        itemCode = intent.getStringExtra("ITEM_CODE") ?: ""

        // Teks awal (bisa pindahkan ke strings.xml)
        binding.tvBookingId.text = "Kode Booking - $bookingId"
        binding.tvFacilityItem.text = itemCode
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCopyBookingId.setOnClickListener {
            copyBookingIdToClipboard()
        }

        binding.btnCancelBooking.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
    }

    private fun copyBookingIdToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Kode Booking", "Kode Booking - $bookingId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Kode booking disalin", Toast.LENGTH_SHORT).show()
    }

    private fun fetchBookingDetails() {
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager
                    .getInstance(this@BookingDetailActivity)
                    .getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getBookingDetails(authHeader, bookingId)

                    if (response.isSuccessful && response.body() != null) {
                        val bookingDetail = response.body()!!.data
                        updateUI(bookingDetail)
                    } else {
                        Toast.makeText(
                            this@BookingDetailActivity,
                            "Gagal memuat detail booking",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@BookingDetailActivity,
                        "Token tidak tersedia. Silakan login ulang.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("BookingDetail", "Error fetching booking details", e)
                Toast.makeText(
                    this@BookingDetailActivity,
                    "Kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUI(bookingDetail: BookingDetail) {
        // Booking ID
        binding.tvBookingId.text = "Kode Booking - ${bookingDetail.id}"

        // Status Booking (Title Case Indonesia)
        binding.tvBookingStatus.text = bookingDetail.status.toTitleCase(localeID)
        updateStatusColor(bookingDetail.status)

        // Facility Item
        binding.tvFacilityItem.text = bookingDetail.facility_item.item_code

        // Gambar fasilitas
        val facilityItemImage = bookingDetail.facility_item.facility_item_image
        if (facilityItemImage != null && !facilityItemImage.image_path.isNullOrEmpty()) {
            ImageUtils.loadImage(
                context = binding.imgFacility.context,
                imagePath = facilityItemImage.image_path,
                imageView = binding.imgFacility
            )
        } else {
            binding.imgFacility.setImageResource(R.drawable.pic_check) // placeholder
        }

        // Jadwal
        try {
            val startDateStr = bookingDetail.start_datetime
            val endDateStr = bookingDetail.end_datetime

            Log.d("BookingDetail", "Original start: $startDateStr, end: $endDateStr")

            val startDateTime = parseApiDateTime(startDateStr)
            val endDateTime = parseApiDateTime(endDateStr)

            if (startDateTime != null) {
                binding.tvStartDate.text = displayDateFormat.format(startDateTime)
                binding.tvStartTime.text = displayTimeFormat.format(startDateTime)
            } else {
                binding.tvStartDate.text = "Tanggal tidak valid"
                binding.tvStartTime.text = "-"
            }

            if (endDateTime != null) {
                binding.tvEndDate.text = displayDateFormat.format(endDateTime)
                binding.tvEndTime.text = displayTimeFormat.format(endDateTime)
            } else {
                binding.tvEndDate.text = "Tanggal tidak valid"
                binding.tvEndTime.text = "-"
            }
        } catch (e: Exception) {
            Log.e("BookingDetail", "Error parsing dates", e)
            binding.tvStartDate.text = "Kesalahan tanggal"
            binding.tvStartTime.text = ""
            binding.tvEndDate.text = "Kesalahan tanggal"
            binding.tvEndTime.text = ""
        }

        // Purpose
        binding.tvPurpose.text = bookingDetail.purpose

        // Tombol batal jika status pending
        if (bookingDetail.status.lowercase(localeID) == "pending") {
            binding.btnCancelBooking.visibility = View.VISIBLE
        } else {
            binding.btnCancelBooking.visibility = View.GONE
            val params = binding.btnContactSupport.layoutParams as LinearLayout.LayoutParams
            params.width = LinearLayout.LayoutParams.MATCH_PARENT
            params.marginStart = 0
            binding.btnContactSupport.layoutParams = params
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Batalkan Booking")
            .setMessage("Yakin ingin membatalkan booking ini?")
            .setPositiveButton("Ya") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun cancelBooking() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager
                    .getInstance(this@BookingDetailActivity)
                    .getAuthToken()

                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.cancelBooking(authHeader, bookingId)

                    showLoading(false)

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@BookingDetailActivity,
                            "Booking berhasil dibatalkan",
                            Toast.LENGTH_SHORT
                        ).show()
                        fetchBookingDetails()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                JSONObject(errorBody).getString("message")
                            } catch (e: Exception) {
                                "Gagal membatalkan booking"
                            }
                        } else {
                            "Gagal membatalkan booking"
                        }
                        Toast.makeText(
                            this@BookingDetailActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this@BookingDetailActivity,
                        "Token tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Log.e("BookingDetail", "Error cancelling booking", e)
                Toast.makeText(
                    this@BookingDetailActivity,
                    "Kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun parseApiDateTime(dateTimeStr: String): Date? {
        val parseLocale = Locale.US
        val possibleFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX" // jika nanti API kirim offset
        ).map { pattern ->
            SimpleDateFormat(pattern, parseLocale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
        }

        for (format in possibleFormats) {
            try {
                val parsed = format.parse(dateTimeStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // try next
            }
        }
        Log.e("BookingDetail", "Gagal parse tanggal: $dateTimeStr")
        return null
    }

    private fun updateStatusColor(status: String) {
        when (status.lowercase(localeID)) {
            "pending" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_pending))
            "approved" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_approved))
            "rejected" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.status_rejected))
            "completed" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.primary_dark_color))
            "cancelled" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.text_gray_color))
            "needs return" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.orange_color))
            "return submitted" -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.purple_color))
            else -> binding.bookingStatusCard.setCardBackgroundColor(getColor(R.color.primary_color))
        }
    }

    // Title case helper
    private fun String.toTitleCase(locale: Locale): String =
        replaceFirstChar { c ->
            if (c.isLowerCase()) c.titlecase(locale) else c.toString()
        }

    @Deprecated("Gunakan onBackPressedDispatcher.onBackPressed() secara langsung di listener.")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}