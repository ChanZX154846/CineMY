package com.fypcinemy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<TextView>(R.id.btnGoRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val settingsPrefs = getSharedPreferences("cinemy_settings", MODE_PRIVATE)

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilEmail.error = getString(R.string.enter_email_reset)
                return@setOnClickListener
            }
            tilEmail.error = null
            resetPassword(email)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = getString(R.string.fill_fields_error)
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tilPassword.error = getString(R.string.fill_fields_error)
                return@setOnClickListener
            }

            signIn(email, password)
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save credentials for biometric login (simulated)
                    val settingsPrefs = getSharedPreferences("cinemy_settings", MODE_PRIVATE)
                    settingsPrefs.edit {
                        putString("last_email", email)
                        putString("last_password", password)
                    }

                    Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    
                    val pin = settingsPrefs.getString("security_pin", null)
                    if (pin == null) {
                        // Force PIN setup
                        startActivity(Intent(this, AppLockActivity::class.java).putExtra("setup_mode", true))
                    } else {
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.auth_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.reset_email_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }
}