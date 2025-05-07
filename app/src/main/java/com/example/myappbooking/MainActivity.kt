package com.example.myappbooking

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myappbooking.databinding.ActivityMainBinding

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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}