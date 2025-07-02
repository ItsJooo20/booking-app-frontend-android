package com.example.myappbooking.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myappbooking.ui.activity.FacilityListActivity
import com.example.myappbooking.R
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.adapter.CategoryAdapter
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.FacilityCategory
import com.example.myappbooking.databinding.FragmentDashboardBinding
import com.example.myappbooking.utility.ImageUtils
import kotlinx.coroutines.launch

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
        setupCategoryAdapter()
        setupSearchView()
        setupReserveButton()
        fetchCategories()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Your custom back press handling here
            requireActivity().finishAffinity() // To exit the app
        }
    }

    private fun setupCategoryAdapter() {
        categoryAdapter = CategoryAdapter(categoryList) { selectedCategory ->

            val intent = Intent(requireContext(), FacilityListActivity::class.java)
            intent.putExtra("CATEGORY_ID", selectedCategory.id)
            intent.putExtra("CATEGORY_NAME", selectedCategory.name)
            startActivity(intent)
        }

        binding.categoriesRecyclerview.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = categoryAdapter
        }
    }

    private fun setupGreetingSection() {
        val prefMan = SharedPreferencesManager.getInstance(requireContext())
        val userName = prefMan.getUserName()
        val imgProfile = prefMan.getUserPhoto()
        binding.greetingText.text = "Hi, $userName!"

        ImageUtils.loadImageWithoutURL(
            context = binding.imageViewProfile.context,
            imagePath = imgProfile,
            imageView = binding.imageViewProfile
        )
    }

    private fun NavigateToReservationFragment() {
        val reservationFragment = ReservationFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, reservationFragment)
            .commit()
    }

    private fun setupReserveButton() {
        binding.extendedFab.setOnClickListener {
//            Toast.makeText(context, "Navigate to reservation screen", Toast.LENGTH_SHORT).show()
            NavigateToReservationFragment()
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
                            val dataCategory = response.body()!!.data

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
        _binding = null
    }
}