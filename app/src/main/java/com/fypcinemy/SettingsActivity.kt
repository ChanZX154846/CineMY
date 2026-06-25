package com.fypcinemy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private lateinit var textNoTickets: TextView
    private lateinit var ticketContainer: LinearLayout
    private var ticketListener: ListenerRegistration? = null

    private lateinit var switchBiometric: Switch

    private val settingsPrefs by lazy {
        getSharedPreferences("cinemy_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settingsScrollView = findViewById<View>(R.id.settingsScrollView)
        val textUserEmail = findViewById<TextView>(R.id.textUserEmail)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        textNoTickets = findViewById(R.id.textNoTickets)
        ticketContainer = findViewById(R.id.ticketContainer)
        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
        val switchRecommendations = findViewById<Switch>(R.id.switchRecommendations)
        val btnSetupPin = findViewById<Button>(R.id.btnSetupPin)
        switchBiometric = findViewById<Switch>(R.id.switchBiometric)
        val btnPaymentMethods = findViewById<Button>(R.id.btnPaymentMethods)
        val btnSupport = findViewById<Button>(R.id.btnSupport)
        val btnAbout = findViewById<Button>(R.id.btnAbout)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        ViewCompat.setOnApplyWindowInsetsListener(settingsScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNav?.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        textUserEmail.text =
            FirebaseAuth.getInstance().currentUser?.email ?: getString(R.string.guest_account)

        renderTickets(TicketStore.getTickets(this))
        ticketListener = TicketStore.listenTickets(
            context = this,
            onTicketsChanged = { tickets -> renderTickets(tickets) },
            onError = {
                Toast.makeText(
                    this,
                    getString(R.string.firebase_ticket_sync_error),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )

        setupSwitch(switchNotifications, "booking_notifications_enabled", true)
        setupSwitch(switchRecommendations, "movie_recommendations_enabled", true)
        
        btnSetupPin.setOnClickListener {
            val intent = Intent(this, AppLockActivity::class.java)
            intent.putExtra("setup_mode", true)
            startActivity(intent)
        }

        setupBiometricSwitch()

        btnEditProfile.setOnClickListener {
            showInfoDialog(
                getString(R.string.edit_profile),
                getString(R.string.edit_profile_message),
            )
        }

        btnPaymentMethods.setOnClickListener {
            showInfoDialog(
                getString(R.string.payment_methods),
                getString(R.string.payment_methods_message),
            )
        }

        btnSupport.setOnClickListener {
            showInfoDialog(
                getString(R.string.help_support),
                getString(R.string.help_support_message),
            )
        }

        btnAbout.setOnClickListener {
            showInfoDialog(
                getString(R.string.about_cinemy),
                getString(R.string.about_cinemy_message),
            )
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
        // Refresh security switch state when returning to settings
        val hasPin = settingsPrefs.getString("security_pin", null) != null
        switchBiometric.isEnabled = hasPin
        
        // Use a temporary listener-less update to avoid triggering registration on resume
        switchBiometric.setOnCheckedChangeListener(null)
        switchBiometric.isChecked = settingsPrefs.getBoolean("biometric_auth_enabled", false)
        setupBiometricSwitch()
    }

    override fun onDestroy() {
        ticketListener?.remove()
        super.onDestroy()
    }

    private fun renderTickets(tickets: List<PurchasedTicket>) {
        ticketContainer.removeAllViews()

        textNoTickets.visibility =
            if (tickets.isEmpty()) View.VISIBLE else View.GONE

        // Limit to top 5 tickets for better layout
        tickets.take(5).forEach { ticket ->
            ticketContainer.addView(createTicketCard(ticket))
        }
    }

    private fun setupBiometricSwitch() {
        switchBiometric.isChecked = settingsPrefs.getBoolean("biometric_auth_enabled", false)

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if PIN is setup first
                val hasPin = settingsPrefs.getString("security_pin", null) != null
                if (!hasPin) {
                    switchBiometric.isChecked = false
                    Toast.makeText(this, "Please set a Security PIN first", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                
                // Trigger "Registration" flow by asking for authentication now
                showBiometricRegistrationPrompt()
            } else {
                settingsPrefs.edit { putBoolean("biometric_auth_enabled", false) }
                Toast.makeText(this, getString(R.string.preference_updated), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBiometricRegistrationPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                settingsPrefs.edit { putBoolean("biometric_auth_enabled", true) }
                Toast.makeText(this@SettingsActivity, getString(R.string.thumbprint_registered_success), Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Temporarily remove listener to avoid triggering it when we revert
                switchBiometric.setOnCheckedChangeListener(null)
                switchBiometric.isChecked = false
                setupBiometricSwitch() // Re-attach
                
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    val message = if (errString.contains("Face", ignoreCase = true)) {
                        "Please use Fingerprint/Thumbprint instead."
                    } else {
                        getString(R.string.thumbprint_registration_failed, errString)
                    }
                    Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        // Use allowed authenticators to prioritize strong/fingerprint
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.register_thumbprint_title))
            .setSubtitle(getString(R.string.register_thumbprint_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupSwitch(
        switch: Switch,
        key: String,
        defaultValue: Boolean,
    ) {
        switch.isChecked = settingsPrefs.getBoolean(key, defaultValue)

        switch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit { putBoolean(key, isChecked) }
            Toast.makeText(
                this,
                getString(R.string.preference_updated),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun createTicketCard(ticket: PurchasedTicket): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.news_card_background)
        card.setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        card.isClickable = true
        card.isFocusable = true
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        card.foreground = getDrawable(outValue.resourceId)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        params.setMargins(0, 12.dp, 0, 0)
        card.layoutParams = params

        card.addView(
            createTicketText(
                ticket.movieTitle,
                20f,
                true,
                0,
            ),
        )

        val showtimeText = ticket.showtime ?: ""
        if (showtimeText.isNotBlank()) {
            card.addView(
                createTicketText(
                    "Time: $showtimeText",
                    16f,
                    false,
                    8.dp,
                ),
            )
        }

        card.addView(
            createTicketText(
                getString(R.string.seats_label, ticket.seats.joinToString(", ")),
                16f,
                false,
                10.dp,
            ),
        )

        card.addView(
            createTicketText(
                getString(R.string.paid_label, ticket.totalPrice),
                16f,
                true,
                8.dp,
            ),
        )

        card.addView(
            createTicketText(
                getString(R.string.booking_id_label, ticket.bookingId),
                15f,
                false,
                8.dp,
            ),
        )

        card.addView(
            createTicketText(
                getString(R.string.purchased_on_label, formatPurchasedDate(ticket.purchasedAt)),
                15f,
                false,
                8.dp,
            ),
        )

        card.setOnClickListener {
            val intent = Intent(this, ETicketActivity::class.java)
            intent.putExtra("movieTitle", ticket.movieTitle)
            intent.putStringArrayListExtra("selectedSeats", ArrayList(ticket.seats))
            intent.putExtra("totalPrice", ticket.totalPrice)
            intent.putExtra("bookingId", ticket.bookingId)
            intent.putExtra("showtime", ticket.showtime)
            startActivity(intent)
        }

        return card
    }

    private fun createTicketText(
        text: String,
        textSize: Float,
        bold: Boolean,
        topPadding: Int,
    ): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = textSize
        textView.setTextColor(getColor(R.color.cinemy_text_primary))
        textView.setPadding(0, topPadding, 0, 0)

        if (bold) {
            textView.setTypeface(textView.typeface, android.graphics.Typeface.BOLD)
        }

        return textView
    }

    private fun formatPurchasedDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
