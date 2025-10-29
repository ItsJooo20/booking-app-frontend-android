package com.example.myappbooking.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myappbooking.data.TokenRequest
import com.example.myappbooking.ui.fragment.ProfileFragment
import com.example.myappbooking.R
import com.example.myappbooking.api.ApiClient
import com.example.myappbooking.ui.fragment.ReservationFragment
import com.example.myappbooking.ui.fragment.ScheduleFragment
import com.example.myappbooking.databinding.ActivityMainBinding
import com.example.myappbooking.helpers.NotificationHelper
import com.example.myappbooking.ui.fragment.DashboardFragment
import com.example.myappbooking.ui.fragment.HistoryFragment
import com.example.myappbooking.utility.NetworkUtils
import com.example.myappbooking.utility.SharedPreferencesManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

    lateinit var binding: ActivityMainBinding

    private val dashboardFragment = DashboardFragment()
    private val scheduleFragment = ScheduleFragment()
    private val historyFragment = HistoryFragment()
    private val profileFragment = ProfileFragment()
    private val reservationFragment = ReservationFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home ->{
                    loadFragment(dashboardFragment)
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(scheduleFragment)
                    true
                }
                R.id.nav_plus -> {
                    loadFragment(reservationFragment)
                    true
                }
                R.id.nav_history -> {
                    loadFragment(historyFragment)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }

        val targetTab = intent.getIntExtra("target_tab", R.id.nav_home)
        binding.bottomNav.selectedItemId = targetTab

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit()
        }

        NotificationHelper.createNotificationChannel(this)

        // Request permission untuk notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            // Langsung ambil FCM token jika di bawah Android 13
            getFCMToken()
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        } else {
            getFCMToken()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                getFCMToken()
            } else {
//                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            // Dapatkan FCM token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Simpan token ke SharedPreferences
            saveTokenToPreferences(token)

            // Kirim token ke backend
            sendTokenToServer(token)
        }
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    private fun sendTokenToServer(token: String) {
        val prefman = SharedPreferencesManager.getInstance(this@MainActivity)
        val userId = prefman.getUserId()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenRequest = TokenRequest(
                    userId = userId,
                    deviceToken = token,
                    deviceType = "android"
                )

                val response = ApiClient.authService.registerToken(tokenRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Token registered successfully")
//                        Toast.makeText(
//                            this@MainActivity,
//                            "Device registered for notifications",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    } else {
                        Log.e(TAG, "Failed to register token: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering token: ${e.message}")
                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@MainActivity,
//                        "Failed to register device",
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    fun setBottomNavEnabled(enabled: Boolean) {
        findViewById<BottomNavigationView>(R.id.bottom_nav)?.isEnabled = enabled
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}