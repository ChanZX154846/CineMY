package com.fypcinemy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            val settingsPrefs = getSharedPreferences("cinemy_settings", MODE_PRIVATE)
            val pin = settingsPrefs.getString("security_pin", null)
            
            if (pin == null) {
                // If logged in but no PIN, force setup
                startActivity(Intent(this, AppLockActivity::class.java).putExtra("setup_mode", true))
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}