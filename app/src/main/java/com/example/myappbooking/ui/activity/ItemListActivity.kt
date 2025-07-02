package com.example.myappbooking.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.adapter.FacilityItemAdapter
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.FacilityItem
import com.example.myappbooking.databinding.ActivityItemListBinding
import kotlinx.coroutines.launch

class ItemListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemListBinding
    private lateinit var itemAdapter: FacilityItemAdapter

    private var facilityId: Int = 0
    private var facilityName: String = ""
    private val facilityItemList = mutableListOf<FacilityItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        facilityId = intent.getIntExtra("FACILITY_ID", 0)
        facilityName = intent.getStringExtra("FACILITY_NAME") ?: ""

        setupViews()
        setupRecyclerView()
        setupSearchView()
        loadFacilityItems(facilityId)
    }

    private fun setupViews() {
        binding.tvCategoryTitle.text = facilityName

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun setupRecyclerView() {
        itemAdapter = FacilityItemAdapter(facilityItemList) { facilityItem ->
//            Toast.makeText(this, "Selected facility: ${facilityItem.item_code}", Toast.LENGTH_SHORT).show()
            // Handle item click - navigate to item detail or perform action
            showItemDetail(facilityItem)
        }

        binding.rvFacilities.apply {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(this@ItemListActivity)

            // Add item decoration if needed
            val itemDecoration = DividerItemDecoration(
                this@ItemListActivity,
                DividerItemDecoration.VERTICAL
            )
            addItemDecoration(itemDecoration)
        }
    }

    private fun setupSearchView() {
        binding.etSearch.hint = "Search items"

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter items based on search query
                itemAdapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle search action
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun loadFacilityItems(facilityId: Int) {
        showLoading(true)

        lifecycleScope.launch {
            lifecycle.whenStarted {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(this@ItemListActivity)
                    val token = prefMan.getAuthToken()

                    if (token != null) {
                        val authHeader = "Bearer $token"
                        val response = ApiClient.authService.getFacilityItems(authHeader, facilityId)

                        if (response.isSuccessful && response.body() != null) {
                            val facilities = response.body()!!.data

                            Log.d("ItemListActivity", "Received ${facilities.size} items")

                            facilityItemList.clear()
                            facilityItemList.addAll(facilities)
                            itemAdapter.notifyDataSetChanged()

                            // Add more detailed logging
                            Log.d("ItemListActivity", "After adapter update, list size: ${facilityItemList.size}")

                            showEmptyState(facilities.isEmpty())
                        } else {
                            showEmptyState(true)
                            Toast.makeText(this@ItemListActivity, "Failed to fetch facilities", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Add detailed error logging
                    Log.e("ItemListActivity", "Error loading items", e)
                    showEmptyState(true)
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun showItemDetail(facilityItem: FacilityItem) {
        val intent = Intent(this, ItemDetailActivity::class.java)
        intent.putExtra("ITEM_ID", facilityItem.id)
        intent.putExtra("ITEM_CODE", facilityItem.item_code)
        intent.putExtra("ITEM_DESC", facilityItem.notes)
        intent.putExtra("ITEM_PRIMARY_IMG", facilityItem.primary_image_url)

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyState.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        showEmptyState(true)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}