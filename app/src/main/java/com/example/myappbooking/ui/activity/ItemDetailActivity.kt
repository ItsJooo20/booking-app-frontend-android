package com.example.myappbooking.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.adapter.GalleryAdapter
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.Images
import com.example.myappbooking.databinding.ActivityItemDetailBinding
import com.example.myappbooking.utility.ImageUtils
import com.example.myappbooking.utility.NetworkUtils
import kotlinx.coroutines.launch

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private lateinit var galleryAdapter: GalleryAdapter

    private var itemId: Int = 0
    private var itemCode: String = ""
    private var itemDesc: String = ""
    private var itemImage: String = ""

    private val imageList = mutableListOf<Images>()
//    private val facilityItemList = mutableListOf<FacilityItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NetworkUtils.init(this)

        itemId = intent.getIntExtra("ITEM_ID", 0)
        itemCode = intent.getStringExtra("ITEM_CODE")?: ""
        itemDesc = intent.getStringExtra("ITEM_DESC")?: ""
        itemImage = intent.getStringExtra("ITEM_PRIMARY_IMG")?: ""

        setupViews()
        setupItem()
        setupGallery()
        setupReserve()
        fetchItemDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
    }

    private fun fetchItemDetails() {
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(this@ItemDetailActivity).getAuthToken()
                if (token != null) {
                    val authHeader = "Bearer $token"
                    val response = ApiClient.authService.getFacilityItemDetail(authHeader, itemId)

                    if (response.isSuccessful && response.body() != null) {
                        val item = response.body()!!.data

                        Log.d("ItemDetail", "Item: $itemId, Images count: ${item.images?.size ?: 0}")

                        item.images?.forEachIndexed { index, image ->
                            Log.d("ItemDetail", "Image $index: id=${image.id}, primary=${image.is_primary}, url=${image.image_path}")
                        }

                        if (!item.images.isNullOrEmpty()) {
                            imageList.clear()
                            imageList.addAll(item.images)
                            galleryAdapter.notifyDataSetChanged()

                            binding.previewHeaderContainer.visibility = View.VISIBLE
                            binding.previewContainer.visibility = View.VISIBLE
                        } else {
                            binding.previewHeaderContainer.visibility = View.GONE
                            binding.previewContainer.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ItemDetail", "Error fetching item details", e)
                // Hide gallery on error
                binding.previewHeaderContainer.visibility = View.GONE
                binding.previewContainer.visibility = View.GONE
            }
        }
    }

    private fun setupGallery() {
        galleryAdapter = GalleryAdapter(imageList) { image ->
            Log.d("ItemDetail", "Image clicked: ${image.image_path}")
        }

        binding.imgItemRv.apply {
            layoutManager = GridLayoutManager(this@ItemDetailActivity, 2)
            adapter = galleryAdapter
        }
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

        ImageUtils.loadImageWithoutURL(
            context = binding.imgItem.context,
            imagePath = itemImage,
            imageView = binding.imgItem
        )
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