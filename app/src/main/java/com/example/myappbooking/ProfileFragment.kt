package com.example.myappbooking

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.example.myappbooking.databinding.FragmentDashboardBinding
import com.example.myappbooking.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreetingSection()
        logout()
    }

    private fun logout() {
        binding.outlinedButton.setOnClickListener {
            setupConfirmation()
        }
    }

    private fun showLoading(show: Boolean) {
        // Show/hide overlay and disable/enable UI
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.isEnabled = !show
        binding.outlinedButton.isEnabled = !show

        // Disable bottom navigation in parent activity
//        (activity as? MainActivity)?.setBottomNavEnabled(!show)
        (activity as? MainActivity)?.apply {
            setBottomNavEnabled(!show)

            // Also add this to block clicks on the bottom nav area
            if (show) {
                binding.loadingOverlay.bringToFront()
            }
        }
    }

    private fun setupConfirmation() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Warning")
            setMessage("Are you sure you want to logout?")
            setPositiveButton("Confirm") { _, _ ->
//                showLoading(true)
                setupLogoutButton()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setCancelable(false)
            show()
        }
    }

    private fun setupGreetingSection() {
        val prefMan = SharedPreferencesManager.getInstance(requireContext())
        val name = prefMan.getUserName()
        val email = prefMan.getUserEmail()
        val role = prefMan.getUserRole()
        binding.textViewName.text = "$name"
        binding.textViewEmail.text = "$email"
        binding.textViewRole.text = "$role"
    }

    private fun setupLogoutButton() {
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.whenStarted {
                try {
                    val prefMan = SharedPreferencesManager.getInstance(requireContext())
                    val token = prefMan.getAuthToken()

                    if (token != null) {
                        val authHeader = "Bearer $token"
                        val response = ApiClient.authService.logout(authHeader)

                        if (response.isSuccessful && response.body() != null) {
                            prefMan.clearUserData()
                            NavigateToLoginPage()
                        } else {
                            prefMan.clearUserData()
                            NavigateToLoginPage()
                        }
                    }
                } catch (e: Exception) {
//                    Toast.makeText(requireContext(), "Failed fetch categories", Toast.LENGTH_SHORT).show()
                } finally {
                    NavigateToLoginPage()
//                    binding.swipeToRefresh
                }
            }
        }
    }

    private fun NavigateToLoginPage() {
        if (isAdded && activity != null) { // Pastikan fragment masih terhubung ke activity
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}