package com.example.myappbooking.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myappbooking.ui.fragment.ProfileFragment
import com.example.myappbooking.R
import com.example.myappbooking.ui.fragment.ReservationFragment
import com.example.myappbooking.ui.fragment.ScheduleFragment
import com.example.myappbooking.databinding.ActivityMainBinding
import com.example.myappbooking.ui.fragment.DashboardFragment
import com.example.myappbooking.ui.fragment.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val dashboardFragment = DashboardFragment()
    private val scheduleFragment = ScheduleFragment()
    private val historyFragment = HistoryFragment()
    private val profileFragment = ProfileFragment()
    private val reservationFragment = ReservationFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, dashboardFragment)
                .commit()
        }

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