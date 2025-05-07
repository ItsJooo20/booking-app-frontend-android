package com.example.myappbooking

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class FacilityListFragment : Fragment() {

    private var _binding: FragmentFacilityListBinding? = null
    private val binding get() = _binding!!

    private lateinit var facilityAdapter: FacilityAdapter
    private val facilityList = mutableListOf<Facility>()

    private var categoryId: Int = 0
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getInt("category_id", 0)
            categoryName = it.getString("category_name", "Facilities")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFacilityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        fetchFacilities()
    }

    private fun setupToolbar() {
        binding.tvCategoryTitle.text = categoryName
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        facilityAdapter = FacilityAdapter(facilityList) { facility ->
            // Handle facility item click - Navigate to detail or reservation
            Toast.makeText(requireContext(), "Selected facility: ${facility.name}", Toast.LENGTH_SHORT).show()
            // Navigate to facility detail or booking screen here
        }

        binding.rvFacilities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = facilityAdapter
        }
    }

    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                facilityAdapter.filter(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })
    }

    private fun fetchFacilities() {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.whenStarted {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(requireContext())
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
                            Toast.makeText(requireContext(), "Failed to fetch facilities", Toast.LENGTH_SHORT).show()
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

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvFacilities.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
