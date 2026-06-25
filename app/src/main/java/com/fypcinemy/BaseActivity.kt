package com.fypcinemy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is HomeActivity) {
                        startActivity(Intent(this, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                    true
                }
                R.id.nav_news -> {
                    if (this !is NewsActivity) {
                        startActivity(Intent(this, NewsActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (this !is SettingsActivity) {
                        startActivity(Intent(this, SettingsActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                    true
                }
                else -> false
            }
        }
    }

    protected fun updateBottomNavigationSelection() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return
        val currentId = when (this) {
            is HomeActivity, is MovieListActivity, is MovieDetailActivity, 
            is SeatSelectionActivity, is CheckoutActivity, is ETicketActivity -> R.id.nav_home
            is NewsActivity -> R.id.nav_news
            is SettingsActivity -> R.id.nav_settings
            else -> null
        }
        if (currentId != null && bottomNavigation.selectedItemId != currentId) {
            // Temporarily disable the listener to prevent navigation loops
            bottomNavigation.setOnItemSelectedListener(null)
            bottomNavigation.selectedItemId = currentId
            setupBottomNavigation() // Re-attach the listener
        }
    }
}
