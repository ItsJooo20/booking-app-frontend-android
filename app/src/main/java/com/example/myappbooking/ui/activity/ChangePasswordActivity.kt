package com.example.myappbooking.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.ChangePasswordRequest
import com.example.myappbooking.databinding.ActivityChangePasswordBinding
import com.example.myappbooking.utility.NetworkUtils
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

        NetworkUtils.init(this)

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

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
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
            binding.tilCurrentPassword.error = "Mohon masukkan password Anda"
            return false
        }

        // Check if new password is empty
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Mohon masukkan password baru"
            return false
        }

        // Validate password strength
        if (!isStrongPassword(newPassword)) {
            binding.tilNewPassword.error = "Password tidak memenuhi persyaratan!"
            return false
        }

        // Check if passwords match
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Konfirmasi Password tidak sama!"
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
            .setMessage("Apakah Anda yakin ingin mengubah password Anda?")
            .setPositiveButton("Ya") { _, _ ->
                updatePassword()
            }
            .setNegativeButton("Batal", null)
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
                    showError("Token tidak ditemukan. Dimohon Login kembali.")
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
                    showSuccessDialog("Password berhasil di update.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
//                            JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            "Gagal memperbarui password"
                        }
                    } else {
                        when (response.code()) {
                            401 -> "Password yang Anda masukkan salah!"
                            422 -> "Gagal menyimpan password"
                            else -> "Gagal memperbarui password"
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
            .setTitle("Sukses!")
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