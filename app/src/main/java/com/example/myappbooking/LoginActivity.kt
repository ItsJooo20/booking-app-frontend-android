package com.example.myappbooking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import kotlin.math.log

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (SharedPreferencesManager.getInstance(this).isLoggedIn()) {
            redirectBasedOnRole()
            finish()
            return
        }
//        else {
//            val prefMan = SharedPreferencesManager.getInstance(this)
//            prefMan.clearUserData()
//        }

        setupLogin()
    }

    private fun setupLogin() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email Required"
                binding.etEmail.requestFocus()
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password Required"
                binding.etPassword.requestFocus()
            }
            else -> performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.login(LoginRequest(email, password))
                }

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        handleSuccessfulLogin(loginResponse)
                    } ?: run {
                        showError("Empty response from server")
                    }
                } else {
                    handleErrorResponse(response)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Invalid Login: $e", Toast.LENGTH_SHORT)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleSuccessfulLogin(loginResponse: LoginResponse) {
        val prefMan = SharedPreferencesManager.getInstance(this)

        loginResponse.data?.let { data ->
            // Save token and user data
            prefMan.saveAuthToken(data.remember_token ?: "")
            prefMan.saveUserData(
                data.user.name ?: "",
                data.user.email ?: "",
                data.user.role ?: ""
            )

            // Redirect based on role
            redirectBasedOnRole()
            finish()
        } ?: showError("No user data in response")
    }

    private fun handleErrorResponse(response: Response<LoginResponse>) {
        try {
            val errorBody = response.errorBody()?.string()
            val errorMessage = errorBody?.let {
                try {
                    JSONObject(it).getString("message")
                } catch (e: Exception) {
                    "Invalid server response: $e"
                }
            } ?: "Unknown error occurred"

            showError(errorMessage)
        } catch (e: Exception) {
            showError("Failed to parse error response")
        }
    }

    private fun redirectBasedOnRole() {
        val role = SharedPreferencesManager.getInstance(this).getUserRole()
        when (role) {
            "user", "admin" -> navigateToMainActivity()
            else -> {
                Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.text = if (show) "Loading..." else "Login"
        binding.tilEmail.isEnabled = !show
        binding.tilPassword.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }


}