package com.example.myappbooking.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import com.example.myappbooking.utility.SharedPreferencesManager
import com.example.myappbooking.databinding.FragmentProfileBinding
import com.example.myappbooking.ui.activity.*
import com.example.myappbooking.utility.ImageUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PROFILE_IMAGE_REQUEST_CODE = 1001
    }

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
        setupProfileImage()
        setupAccountSettings()
        setupLogoutButton()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Your custom back press handling here
            requireActivity().finishAffinity() // To exit the app
        }
    }

    private fun setupProfileImage() {
        // Make the profile image clickable
        binding.cardViewProfile.setOnClickListener {
            openProfileImageScreen()
        }

        binding.imageViewProfile.setOnClickListener {
            openProfileImageScreen()
        }

        // Load the profile image
        loadProfileImage()
    }

    private fun openProfileImageScreen() {
        val intent = Intent(requireContext(), ProfileImageActivity::class.java)
        startActivityForResult(intent, PROFILE_IMAGE_REQUEST_CODE)
    }

    private fun loadProfileImage() {

        val prefman = SharedPreferencesManager.getInstance(requireContext())
        val profilePic = prefman.getUserPhoto()

        ImageUtils.loadImageWithoutURL(
            context = binding.imageViewProfile.context,
            imagePath = profilePic,
            imageView = binding.imageViewProfile
        )
//        lifecycleScope.launch {
//            try {
//                val prefMan = SharedPreferencesManager.getInstance(requireContext())
//                val token = prefMan.getAuthToken()
//
//                if (token.isNullOrEmpty()) {
//                    return@launch
//                }
//
//                val authHeader = "Bearer $token"
//                val response = ApiClient.authService.getUserProfile(authHeader)
//
//                if (response.isSuccessful && response.body() != null) {
//                    val userData = response.body()!!.data
//
//                    // Load profile image if available
//                    if (!userData.profile_image.isNullOrEmpty()) {
//                        context?.let { ctx ->
//                            Glide.with(ctx)
//                                .load(userData.profile_image)
//                                .placeholder(R.drawable.ic_profile)
//                                .error(R.drawable.ic_profile)
//                                .circleCrop()
//                                .into(binding.imageViewProfile)
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                // Handle error silently - just keep default profile image
//            }
//        }
    }

    private fun setupAccountSettings() {
        binding.cardChangePass.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        binding.cardChangePhone.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePhoneNumberActivity::class.java))
        }
    }

    private fun setupLogoutButton() {
        binding.outlinedButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLoading(show: Boolean) {
        // Show/hide overlay and disable/enable UI
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.outlinedButton.isEnabled = !show

        // Disable bottom navigation in parent activity
//        (activity as? MainActivity)?.apply {
//            setBottomNavEnabled(!show)
//
//            // Also add this to block clicks on the bottom nav area
//            if (show) {
//                binding.loadingOverlay.bringToFront()
//            }
//        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Warning")
            setMessage("Are you sure you want to logout?")
            setPositiveButton("Confirm") { _, _ ->
                performLogout()
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
        binding.textViewName.text = name
        binding.textViewEmail.text = email
        binding.textViewRole.text = role
    }

    private fun performLogout() {
        showLoading(true)
        val prefMan = SharedPreferencesManager.getInstance(requireContext())
//        val token = prefMan.getAuthToken()
        prefMan.clearUserData()
        navigateToLoginPage()

//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.lifecycle.whenStarted {
//                try {
//                    val prefMan = SharedPreferencesManager.getInstance(requireContext())
//                    val token = prefMan.getAuthToken()
//
//                    if (token != null) {
//                        val authHeader = "Bearer $token"
//                        val response = ApiClient.authService.logout(authHeader)
//
//                        if (response.isSuccessful && response.body() != null) {
//                            prefMan.clearUserData()
//                            navigateToLoginPage()
//                        } else {
//                            prefMan.clearUserData()
//                            navigateToLoginPage()
//                        }
//                    }
//                } catch (e: Exception) {
//                    navigateToLoginPage()
//                } finally {
//                    navigateToLoginPage()
//                }
//            }
//        }
    }

    private fun navigateToLoginPage() {
        if (isAdded && activity != null) { // Make sure fragment is still attached to activity
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh profile image
            val path = data?.getStringExtra("profile_image_path")
//            Toast.makeText(requireContext(), path, Toast.LENGTH_SHORT).show()
            loadProfileImage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}