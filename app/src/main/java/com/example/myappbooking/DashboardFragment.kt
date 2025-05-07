package com.example.myappbooking

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myappbooking.databinding.FragmentDashboardBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: CategoryAdapter

    private val categoryList = mutableListOf<FacilityCategory>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreetingSection()
        setupCategoryRecyclerView()
        setupSearchView()
        setupReserveButton()
        fetchCategories()
    }

    private fun setupGreetingSection() {
        val prefMan = SharedPreferencesManager.getInstance(requireContext()).getUserName()
        binding.greetingText.text = "Hi, $prefMan"
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter(categoryList) { selectedCategory ->
            // Panggil fungsi untuk navigasi
            openFacilityListFragment(selectedCategory)
        }

        binding.categoriesRecyclerview.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoryAdapter
        }
    }

    private fun openFacilityListFragment(category: FacilityCategory) {
        val bundle = Bundle().apply {
            putInt("category_id", category.id)
            putString("category_name", category.name)
        }

        val facilityListFragment = FacilityListFragment().apply {
            arguments = bundle
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, facilityListFragment)
            .addToBackStack(null)
            .commit()
    }



    private fun setupReserveButton() {
        binding.extendedFab.setOnClickListener {
            Toast.makeText(context, "Navigate to reservation screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchView() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                categoryAdapter.filter(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed
            }
        })
    }

    private fun fetchCategories() {
//        binding.swipeRefresh
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.whenStarted {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(requireContext())
                    val token = prefMan.getAuthToken()

                    if (token != null) {
                        val authHeader = "Bearer $token"
                        val response = ApiClient.authService.getCategories(authHeader)

                        if (response.isSuccessful && response.body() != null) {
                            val dataCategory =response.body()!!.data

                            categoryList.clear()
                            categoryList.addAll(dataCategory)
                            categoryAdapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(requireContext(), "Failed fetch categories", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
//                    Toast.makeText(requireContext(), "Failed fetch categories", Toast.LENGTH_SHORT).show()
                } finally {
//                    binding.swipeToRefresh
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}