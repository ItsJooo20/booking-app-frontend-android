package com.example.myappbooking.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.ChangePhoneRequest
import com.example.myappbooking.databinding.ActivityChangePhoneNumberBinding
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChangePhoneNumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePhoneNumberBinding
    private val authApiService = ApiClient.authService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadCurrentPhoneNumber()
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

        // Update Phone button
        binding.btnUpdatePhone.setOnClickListener {
            if (validateInputs()) {
                showConfirmationDialog()
            }
        }
    }

    private fun loadCurrentPhoneNumber() {
        val prefman = SharedPreferencesManager.getInstance(this@ChangePhoneNumberActivity)
        val phone = prefman.getUserPhone()

        if (phone.isNullOrEmpty()) {
            binding.tvCurrentPhone.text = "Add your phone number"
        } else {
            binding.tvCurrentPhone.text = phone
        }
    }

    private fun validateInputs(): Boolean {
        val newPhone = binding.etNewPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Reset errors
        binding.tilNewPhone.error = null
        binding.tilPassword.error = null

        // Check if new phone is empty
        if (newPhone.isEmpty()) {
            binding.tilNewPhone.error = "Please enter a phone number"
            return false
        }

        // Validate phone number format (basic validation)
        if (!isValidPhoneNumber(newPhone)) {
            binding.tilNewPhone.error = "Please enter a valid phone number"
            return false
        }

        // Check if current password is empty
        if (password.isEmpty()) {
            binding.tilPassword.error = "Please enter your current password"
            return false
        }

        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Simple validation - adjust as needed for your requirements
        val phonePattern = "^[+]?[0-9]{10,15}$"
        return phone.matches(phonePattern.toRegex())
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Update Phone Number")
            .setMessage("Are you sure you want to change your phone number?")
            .setPositiveButton("Confirm") { _, _ ->
                updatePhoneNumber()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun updatePhoneNumber() {
        val newPhone = binding.etNewPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        showLoading(true)

        lifecycleScope.launch {
            try {
                val prefMan = SharedPreferencesManager.getInstance(this@ChangePhoneNumberActivity)
                val token = prefMan.getAuthToken()

                if (token.isNullOrEmpty()) {
                    showLoading(false)
                    showError("Authentication token not found. Please login again.")
                    return@launch
                }

                val authHeader = "Bearer $token"
                val requestBody = ChangePhoneRequest(
                    current_password = password,
                    new_phone = newPhone
                )

                val response = authApiService.changePhoneNumber(authHeader, requestBody)

                showLoading(false)

                if (response.isSuccessful) {
                    showSuccessDialog("Your phone number has been updated successfully.")
                    prefMan.saveUpdatedPhone(newPhone)
                    loadCurrentPhoneNumber()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            "Failed to update phone number"
                        }
                    } else {
                        when (response.code()) {
                            401 -> "Current password is incorrect"
                            422 -> "This phone number is already in use by another account"
                            else -> "Failed to update phone number"
                        }
                    }
                    showError(errorMessage)
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
        binding.etNewPhone.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.btnUpdatePhone.isEnabled = !show
        binding.btnCancel.isEnabled = !show
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}