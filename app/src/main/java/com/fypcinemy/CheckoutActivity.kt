package com.fypcinemy

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.fypcinemy.notifications.NotificationHelper

class CheckoutActivity : BaseActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                RecommendationNotifier.showRecommendationsIfAvailable(
                    this,
                    TicketStore.getTickets(this),
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        val textMovieTitle = findViewById<TextView>(R.id.textCheckoutMovie)
        val textSeats = findViewById<TextView>(R.id.textCheckoutSeats)
        val textTotal = findViewById<TextView>(R.id.textTotalPrice)

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val spinnerPayment = findViewById<Spinner>(R.id.spinnerPayment)
        val btnPayNow = findViewById<Button>(R.id.btnPayNow)

        val movieTitle = intent.getStringExtra("movieTitle")
        val movieGenre = intent.getStringExtra("movieGenre")
        val showtime = intent.getStringExtra("showtime")

        val selectedSeats =
            intent.getStringArrayListExtra("selectedSeats")

        textMovieTitle.text = movieTitle

        textSeats.text =
            getString(R.string.seats_label, selectedSeats?.joinToString(", ") ?: "")

        val totalPrice = (selectedSeats?.size ?: 0) * 20

        textTotal.text = getString(R.string.total_label, totalPrice)

        val paymentMethods = arrayOf(
            "PayPal",
            "Credit Card",
            "Touch 'n Go",
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            paymentMethods,
        )

        spinnerPayment.adapter = adapter

        checkAndRequestNotificationPermission()
        setupBottomNavigation()

        btnPayNow.setOnClickListener {

            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth
                .getInstance()
                .currentUser

            val bookingData = hashMapOf(
                "movieTitle" to movieTitle,
                "genre" to movieGenre,
                "selectedSeats" to selectedSeats,
                "totalPrice" to totalPrice,
                "userEmail" to currentUser?.email,
                "bookingTime" to System.currentTimeMillis(),
                "showtime" to showtime
            )

            firestore.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener { documentReference ->
                    val bookingId = documentReference.id
                    TicketStore.addTicket(
                        this,
                        movieTitle ?: "",
                        movieGenre,
                        selectedSeats ?: emptyList(),
                        totalPrice,
                        bookingId,
                        showtime
                    )

                    sendRecommendationNotification()
                    NotificationHelper.showBookingNotification(this, movieTitle ?: "", bookingId)

                    val intent = Intent(
                        this,
                        ETicketActivity::class.java
                    )

                    intent.putExtra(
                        "movieTitle",
                        movieTitle
                    )

                    intent.putExtra(
                        "selectedSeats",
                        selectedSeats
                    )

                    intent.putExtra(
                        "totalPrice",
                        totalPrice
                    )

                    intent.putExtra(
                        "bookingId",
                        bookingId
                    )

                    intent.putExtra(
                        "showtime",
                        showtime
                    )

                    startActivity(intent)

                    finish()
                }

                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Booking save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
    }

    private fun sendRecommendationNotification() {
        if (RecommendationNotifier.hasNotificationPermission(this)) {
            RecommendationNotifier.showRecommendationsIfAvailable(
                this,
                TicketStore.getTickets(this),
            )
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (!RecommendationNotifier.hasNotificationPermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
