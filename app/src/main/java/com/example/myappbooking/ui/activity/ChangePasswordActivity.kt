package com.example.myappbooking.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.ChangePasswordRequest
import com.example.myappbooking.databinding.ActivityChangePasswordBinding
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val authApiService = ApiClient.authService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            onBackPressed()
        }

        // Update Password button
        binding.btnUpdatePassword.setOnClickListener {
            if (validateInputs()) {
                showConfirmationDialog()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Reset errors
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        // Check if current password is empty
        if (currentPassword.isEmpty()) {
            binding.tilCurrentPassword.error = "Please enter your current password"
            return false
        }

        // Check if new password is empty
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Please enter a new password"
            return false
        }

        // Validate password strength
        if (!isStrongPassword(newPassword)) {
            binding.tilNewPassword.error = "Password doesn't meet requirements"
            return false
        }

        // Check if passwords match
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords don't match"
            return false
        }

        return true
    }

    private fun isStrongPassword(password: String): Boolean {
        // At least 8 characters
        if (password.length < 8) return false

        // Contains uppercase and lowercase
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        if (!hasUppercase || !hasLowercase) return false

        // Contains at least one number
        val hasNumber = password.any { it.isDigit() }
        if (!hasNumber) return false

        // Contains at least one special character
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        if (!hasSpecial) return false

        return true
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Update Password")
            .setMessage("Are you sure you want to change your password?")
            .setPositiveButton("Confirm") { _, _ ->
                updatePassword()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun updatePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()

        showLoading(true)

        lifecycleScope.launch {
            try {
                val prefMan = SharedPreferencesManager.getInstance(this@ChangePasswordActivity)
                val token = prefMan.getAuthToken()

                if (token.isNullOrEmpty()) {
                    showLoading(false)
                    showError("Authentication token not found. Please login again.")
                    return@launch
                }

                val authHeader = "Bearer $token"
                val requestBody = ChangePasswordRequest(
                    current_password = currentPassword,
                    new_password = newPassword,
                    new_password_confirmation = newPassword
                )

                val response = authApiService.changePassword(authHeader, requestBody)

                showLoading(false)

                if (response.isSuccessful) {
                    showSuccessDialog("Your password has been updated successfully.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
//                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            "Failed to update password"
                        }
                    } else {
                        when (response.code()) {
                            401 -> "Current password is incorrect"
                            422 -> "Invalid input provided"
                            else -> "Failed to update password"
                        }
                    }
//                    showError(errorMessage)
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE

        // Disable UI elements when loading
        binding.etCurrentPassword.isEnabled = !show
        binding.etNewPassword.isEnabled = !show
        binding.etConfirmPassword.isEnabled = !show
        binding.btnUpdatePassword.isEnabled = !show
        binding.btnCancel.isEnabled = !show
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}