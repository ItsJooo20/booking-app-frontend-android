package com.example.myappbooking.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myappbooking.databinding.ActivitySuccessSentBinding

class SuccessSentActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessSentBinding
    private var itemCode: String = ""
    private var bookingId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessSentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup()
        navigateToDetail()
    }

    private fun navigateToDetail() {
        binding.btnViewBookingDetails.setOnClickListener {
            val intent = Intent(this, BookingDetailActivity::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            intent.putExtra("ITEM_CODE", itemCode)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setup() {
        bookingId = intent.getIntExtra("BOOKING_ID", 0)
        itemCode = intent.getStringExtra("ITEM_CODE") ?: ""

        binding.tvBookingReference.text = itemCode

        binding.btnBacktoHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}