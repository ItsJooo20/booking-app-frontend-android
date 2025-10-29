package com.example.myappbooking.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.data.EmailApp
import com.example.myappbooking.data.EmailRequest
import com.example.myappbooking.utility.NetworkUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var tvEmailAddress: TextView
    private lateinit var btnOpenEmail: MaterialButton
    private lateinit var btnResendEmail: MaterialButton
    private lateinit var tvBackToLogin: TextView
    private lateinit var tvTop: TextView
//    private lateinit var loadingOverlay: FrameLayout

    private var userEmail: String = ""
    private var userName: String = ""
    private var emailVerif: String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        NetworkUtils.init(this)

        // Get email from intent
        userEmail = intent.getStringExtra("user_email") ?: ""
        userName = intent.getStringExtra("user_name") ?: ""
        emailVerif = intent.getStringExtra("tv_top") ?: ""

        initViews()
        displayUserEmail()
        setupClickListeners()
    }

    private fun initViews() {
        tvEmailAddress = findViewById(R.id.tvEmailAddress)
        btnOpenEmail = findViewById(R.id.btnOpenEmail)
        btnResendEmail = findViewById(R.id.btnResendEmail)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
        tvTop = findViewById(R.id.tvSuccessTop)
//        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
    }

    private fun setupClickListeners() {
        btnOpenEmail.setOnClickListener {
            openEmailApp()
        }

        btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        tvBackToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun displayUserEmail() {
        tvTop.text = emailVerif
        tvEmailAddress.text = userEmail
    }

    private fun openGmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.setClassName("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka Gmail", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openEmailApp() {
        val emailApps = listOf(
            EmailApp("Gmail", "com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail"),
            EmailApp("Outlook", "com.microsoft.office.outlook", "com.microsoft.office.outlook.MainActivity"),
            EmailApp("Yahoo Mail", "com.yahoo.mobile.client.android.mail", "com.yahoo.mobile.client.android.mail.ui.MailboxActivity"),
            EmailApp("Blue Mail", "me.bluemail.mail", "me.bluemail.mail.MainActivity"),
            EmailApp("K-9 Mail", "com.fsck.k9", "com.fsck.k9.activity.MessageList"),
            EmailApp("ProtonMail", "ch.protonmail.android", "ch.protonmail.android.activities.mailbox.MailboxActivity"),
            EmailApp("Spark", "com.readdle.spark", "com.readdle.spark.MainActivity"),
            EmailApp("Edison Mail", "com.easilydo.mail", "com.easilydo.mail.activity.MainActivity")
        )

        val availableApps = emailApps.filter { isAppInstalled(it.packageName) }

        when {
            availableApps.isEmpty() -> {
                // Fallback to default email intent
                openDefaultEmailApp()
            }
            availableApps.size == 1 -> {
                // Only one email app available, open it directly
                openSpecificEmailApp(availableApps[0])
            }
            else -> {
                // Multiple apps available, show selection dialog
                showEmailAppSelectionDialog(availableApps)
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun openSpecificEmailApp(emailApp: EmailApp) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.setClassName(emailApp.packageName, emailApp.activityName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka ${emailApp.name}", Toast.LENGTH_SHORT).show()
            // Fallback to package manager launch
            openAppByPackage(emailApp.packageName, emailApp.name)
        }
    }

    private fun openAppByPackage(packageName: String, appName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tidak bisa membuka $appName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka $appName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmailAppSelectionDialog(availableApps: List<EmailApp>) {
        val appNames = availableApps.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pilih Aplikasi Email")
            .setItems(appNames) { _, which ->
                openSpecificEmailApp(availableApps[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openDefaultEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            // Final fallback - open email compose
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.data = Uri.parse("mailto:")
                startActivity(Intent.createChooser(emailIntent, "Pilih aplikasi email"))
            } catch (ex: Exception) {
                Toast.makeText(this, "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendVerificationEmail() {
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Email address not found", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Replace with your actual API call for resending verification email
                    ApiClient.authService.resendVerifEmail(
                        EmailRequest(email = userEmail)
                    )
                }

                if (response.isSuccessful) {
                    HandleResendSuccess()
                    Toast.makeText(
                        this@EmailVerificationActivity,
                        "Verification email sent successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@EmailVerificationActivity,
                        "Failed to send verification email. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EmailVerificationActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun HandleResendSuccess() {
        tvTop.text = "Resend Verification Email Success!"
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
//        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        btnResendEmail.isEnabled = !show
        btnOpenEmail.isEnabled = !show
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate to login instead of going back to registration
        navigateToLogin()
    }
}