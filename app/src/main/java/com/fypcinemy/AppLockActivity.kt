package com.fypcinemy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class AppLockActivity : AppCompatActivity() {

    private var currentPin = ""
    private lateinit var dotViews: List<ImageView>
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    
    private var isSetupMode = false
    private var firstPinEntry = ""

    private lateinit var pinEntryContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app_lock)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        isSetupMode = intent.getBooleanExtra("setup_mode", false)
        pinEntryContainer = findViewById(R.id.pinEntryContainer)
        
        dotViews = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        setupKeypad()
        setupBiometric()

        val tvForgotPin = findViewById<TextView>(R.id.tvForgotPin)
        val prefs = getSharedPreferences("cinemy_settings", MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean("biometric_auth_enabled", false)
        
        if (isSetupMode) {
            findViewById<TextView>(R.id.tvLockTitle).text = getString(R.string.setup_pin)
            tvForgotPin.visibility = View.GONE
            pinEntryContainer.visibility = View.VISIBLE
        } else {
            if (biometricEnabled) {
                // Biometric First flow: Hide PIN until cancelled
                pinEntryContainer.visibility = View.GONE
                biometricPrompt.authenticate(promptInfo)
            } else {
                pinEntryContainer.visibility = View.VISIBLE
            }

            tvForgotPin.setOnClickListener {
                showPasswordVerificationDialog()
            }
        }
    }

    private fun setupKeypad() {
        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        buttons.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                if (currentPin.length < 4) {
                    val digit = (it as Button).text.toString()
                    currentPin += digit
                    updateDots()
                    if (currentPin.length == 4) {
                        handlePinComplete()
                    }
                }
            }
        }

        findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.substring(0, currentPin.length - 1)
                updateDots()
            }
        }
    }

    private fun updateDots() {
        dotViews.forEachIndexed { index, imageView ->
            if (index < currentPin.length) {
                imageView.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                imageView.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }
    }

    private fun handlePinComplete() {
        val prefs = getSharedPreferences("cinemy_settings", MODE_PRIVATE)
        Log.d("AppLock", "PIN Complete. SetupMode: $isSetupMode")
        
        if (isSetupMode) {
            if (firstPinEntry.isEmpty()) {
                firstPinEntry = currentPin
                currentPin = ""
                updateDots()
                findViewById<TextView>(R.id.tvLockSubtitle).text = getString(R.string.reenter_pin)
            } else {
                if (currentPin == firstPinEntry) {
                    Log.d("AppLock", "PIN Setup success. Saving PIN.")
                    prefs.edit().putString("security_pin", currentPin).apply()
                    Toast.makeText(this, getString(R.string.pin_setup_success), Toast.LENGTH_SHORT).show()
                    unlockSuccess()
                } else {
                    Log.d("AppLock", "PIN Setup mismatch.")
                    Toast.makeText(this, getString(R.string.pin_mismatch), Toast.LENGTH_SHORT).show()
                    currentPin = ""
                    firstPinEntry = ""
                    updateDots()
                    findViewById<TextView>(R.id.tvLockSubtitle).text = getString(R.string.enter_pin)
                }
            }
        } else {
            val savedPin = prefs.getString("security_pin", "")
            Log.d("AppLock", "Verifying PIN. Saved: $savedPin, Entered: $currentPin")
            if (currentPin == savedPin) {
                Log.d("AppLock", "PIN Correct. Unlocking.")
                unlockSuccess()
            } else {
                Log.d("AppLock", "PIN Incorrect.")
                Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
                currentPin = ""
                updateDots()
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If cancelled or failed, show PIN fallback
                    Log.d("AppLock", "Biometric Error: $errorCode - $errString. Showing PIN fallback.")
                    pinEntryContainer.visibility = View.VISIBLE
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // If thumbprint fails, we can either keep trying or show PIN. 
                    // Usually, for "Biometric First", we stay on biometric until they explicitly cancel (Error).
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_prompt_negative)) // Standard cancellation option
            .build()
    }

    private fun showPasswordVerificationDialog() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val editText = EditText(this)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.hint = "Password"

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.verify_password_title))
            .setMessage(getString(R.string.verify_password_desc))
            .setView(editText)
            .setPositiveButton(getString(R.string.reset_pin)) { _, _ ->
                val password = editText.text.toString()
                if (password.isNotEmpty()) {
                    verifyPassword(user.email!!, password)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun verifyPassword(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.password_verified), Toast.LENGTH_SHORT).show()
                    startPinReset()
                } else {
                    Toast.makeText(this, getString(R.string.password_incorrect), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startPinReset() {
        isSetupMode = true
        currentPin = ""
        firstPinEntry = ""
        updateDots()
        pinEntryContainer.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvLockTitle).text = getString(R.string.setup_pin)
        findViewById<TextView>(R.id.tvLockSubtitle).text = getString(R.string.enter_pin)
        findViewById<TextView>(R.id.tvForgotPin).visibility = View.GONE
    }

    private fun unlockSuccess() {
        Log.d("AppLock", "UnlockSuccess called. isTaskRoot: $isTaskRoot")
        val app = application as? CineMyApplication
        app?.onUnlocked()
        
        if (isTaskRoot) {
            Log.d("AppLock", "Starting MainActivity from root.")
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}