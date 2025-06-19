package com.example.myappbooking

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myappbooking.databinding.ActivityItemDetailBinding

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding

    private var itemId: Int = 0
    private var itemCode: String = ""
    private var itemDesc: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getIntExtra("ITEM_ID", 0)
        itemCode = intent.getStringExtra("ITEM_CODE")?: ""
        itemDesc = intent.getStringExtra("ITEM_DESC")?: ""

        setupViews()
        setupItem()
        setupReserve()
    }

    private fun setupReserve() {
        binding.btnReserve.setOnClickListener {
            val intent = Intent(this, ReservationByItemActivity::class.java)
            intent.putExtra("ITEM_ID", itemId)
            intent.putExtra("ITEM_CODE", itemCode)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupItem() {
        binding.tvItem.text = itemCode
        binding.tvItemDescription.text = itemDesc
    }

    private fun setupViews() {
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