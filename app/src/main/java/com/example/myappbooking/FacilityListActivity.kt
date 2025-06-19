package com.example.myappbooking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myappbooking.databinding.ActivityFacilityListBinding
import kotlinx.coroutines.launch

class FacilityListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFacilityListBinding
    private lateinit var facilityAdapter: FacilityAdapter
    private var categoryId: Int = 0
    private var categoryName: String = ""
    private val facilityList = mutableListOf<Facility>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacilityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        categoryId = intent?.getIntExtra("CATEGORY_ID", 0) ?: 0
        categoryName = intent?.getStringExtra("CATEGORY_NAME") ?: ""

        setupViews()
        setupRecyclerView()
        setupSearchView()
        loadFacilitiesByCategory(categoryId)
    }

    private fun setupViews() {
        // Set category title
        binding.tvCategoryTitle.text = categoryName

        // Setup back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Setup filter button (if needed)
        binding.btnFilter.setOnClickListener {
            // Handle filter action
        }
    }

    private fun setupRecyclerView() {
        facilityAdapter = FacilityAdapter(facilityList) { facility ->
            // Handle facility item click
//            Toast.makeText(this, "Selected facility: ${facility.name}", Toast.LENGTH_SHORT).show()
            // Navigate to facility detail or perform action
            val intent = Intent(this, ItemListActivity::class.java)
            intent.putExtra("FACILITY_ID", facility.id)
            intent.putExtra("FACILITY_NAME", facility.name)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.rvFacilities.apply {
            adapter = facilityAdapter
            layoutManager = LinearLayoutManager(this@FacilityListActivity)

            // Add item decoration if needed
            val itemDecoration = DividerItemDecoration(
                this@FacilityListActivity,
                DividerItemDecoration.VERTICAL
            )
            addItemDecoration(itemDecoration)
        }
    }

    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter facilities based on search query
                facilityAdapter.filter(s.toString())
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

    private fun loadFacilitiesByCategory(categoryId: Int) {
        showLoading(true)

        lifecycleScope.launch {
            lifecycle.whenStarted {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(this@FacilityListActivity)
                    val token = prefMan.getAuthToken()

                    if (token != null) {
                        val authHeader = "Bearer $token"
                        val response = ApiClient.authService.getFacilities(authHeader, categoryId)

                        if (response.isSuccessful && response.body() != null) {
                            val facilities = response.body()!!.data

                            facilityList.clear()
                            facilityList.addAll(facilities)
                            facilityAdapter.notifyDataSetChanged()

                            showEmptyState(facilities.isEmpty())
                        } else {
                            showEmptyState(true)
                            Toast.makeText(this@FacilityListActivity, "Failed to fetch facilities", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    showEmptyState(true)
//                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        }

    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyState.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (show) View.GONE else View.VISIBLE
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