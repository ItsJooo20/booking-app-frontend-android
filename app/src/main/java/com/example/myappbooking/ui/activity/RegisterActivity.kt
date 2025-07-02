package com.example.myappbooking.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.RegisterRequest
import com.example.myappbooking.data.RegisterResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLogin: TextView
    private lateinit var overlay: FrameLayout
    private lateinit var progressbar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
        logIn()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // This will close all activities and exit the app
        finishAffinity()
    }


    private fun logIn() {
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        overlay = findViewById(R.id.loadingOverlay)
        progressbar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        // Validate name
        if (TextUtils.isEmpty(name)) {
            etName.error = "Name is required"
            etName.requestFocus()
            return false
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            etEmail.requestFocus()
            return false
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            etPhone.error = "Phone number is required"
            etPhone.requestFocus()
            return false
        }

        if (phone.length < 10) {
            etPhone.error = "Enter a valid phone number"
            etPhone.requestFocus()
            return false
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            etPassword.requestFocus()
            return false
        }

        // Validate password confirmation
        if (TextUtils.isEmpty(passwordConfirm)) {
            etPasswordConfirm.error = "Password confirmation is required"
            etPasswordConfirm.requestFocus()
            return false
        }

        if (password != passwordConfirm) {
            etPasswordConfirm.error = "Passwords do not match"
            etPasswordConfirm.requestFocus()
            return false
        }

        // Validate terms checkbox
        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun performRegistration() {
        showLoading(true)

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
//        val passwordConfirm = etPasswordConfirm.text.toString().trim()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.register(
                        RegisterRequest(
                            name = name,
                            email = email,
                            password = password,
                            password_confirmation = password,
//                            passwordConfirmation = passwordConfirm,
                            phone = phone,
                            role = "user"
                        )
                    )
                }

                if (response.isSuccessful) {
                    response.body()?.let { registerResponse ->
                        handleSuccessfulRegistration(registerResponse, email)
                    } ?: run {
                        showError("Empty response from server")
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, "Failed to register, please try again!", Toast.LENGTH_SHORT).show()
//                    handleErrorResponse(response)
                }
            } catch (e: Exception) {
                showError("Registration failed: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleSuccessfulRegistration(response: RegisterResponse, userEmail: String) {
        Toast.makeText(this, response.message, Toast.LENGTH_LONG).show()
        val txt = "Account Created Successfully!"
        // Navigate to email verification screen
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra("tv_top", txt)
        intent.putExtra("user_email", userEmail)
        intent.putExtra("user_name", etName.text.toString().trim())
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        overlay.visibility = if (show) View.VISIBLE else View.GONE
        progressbar.visibility = if (show) View.VISIBLE else View.GONE
        etName.isEnabled = !show
        etEmail.isEnabled = !show
        etPhone.isEnabled = !show
        etPassword.isEnabled = !show
        etPasswordConfirm.isEnabled = !show
        cbTerms.isEnabled = !show
        btnRegister.isEnabled = !show
        btnRegister.text = if (show) "Creating Account..." else "Create Account"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}