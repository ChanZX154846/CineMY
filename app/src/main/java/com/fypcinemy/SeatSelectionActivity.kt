package com.fypcinemy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SeatSelectionActivity : BaseActivity() {

    companion object {
        const val EXTRA_MOVIE_TITLE = "title"
        const val EXTRA_MOVIE_GENRE = "genre"
    }

    private lateinit var textSelectedSeats: TextView

    private val selectedSeats = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        val movieTitle = intent.getStringExtra(EXTRA_MOVIE_TITLE).orEmpty()
        val movieGenre = intent.getStringExtra(EXTRA_MOVIE_GENRE).orEmpty()
        var ticketQuantity = 1

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val textMovieTitle = findViewById<TextView>(R.id.textSeatMovieTitle)
        textSelectedSeats = findViewById(R.id.textSelectedSeats)
        val txtQuantity = findViewById<TextView>(R.id.txtQuantity)
        val btnPlus = findViewById<android.widget.ImageButton>(R.id.btnPlus)
        val btnMinus = findViewById<android.widget.ImageButton>(R.id.btnMinus)

        textMovieTitle.text = movieTitle
        
        fun updateQuantityUI() {
            txtQuantity.text = ticketQuantity.toString()
            textSelectedSeats.text = if (selectedSeats.isEmpty()) {
                "Please select $ticketQuantity seat(s)"
            } else {
                getString(R.string.selected_seats_label, selectedSeats.joinToString(", ")) + " ($ticketQuantity seat(s) required)"
            }
        }

        updateQuantityUI()

        val seatA1 = findViewById<Button>(R.id.seatA1)
        val seatA2 = findViewById<Button>(R.id.seatA2)
        val seatA3 = findViewById<Button>(R.id.seatA3)
        val seatB1 = findViewById<Button>(R.id.seatB1)
        val seatB2 = findViewById<Button>(R.id.seatB2)
        val seatB3 = findViewById<Button>(R.id.seatB3)

        btnPlus.setOnClickListener {
            if (ticketQuantity < 10) {
                ticketQuantity++
                updateQuantityUI()

        val seatA1 = findViewById<Button>(R.id.seatA1)
        val seatA2 = findViewById<Button>(R.id.seatA2)
        val seatA3 = findViewById<Button>(R.id.seatA3)
        val seatB1 = findViewById<Button>(R.id.seatB1)
        val seatB2 = findViewById<Button>(R.id.seatB2)
        val seatB3 = findViewById<Button>(R.id.seatB3)
            }
        }

        btnMinus.setOnClickListener {
            if (ticketQuantity > 1) {
                ticketQuantity--
                // If current selected exceeds new limit, clear selection or remove last
                while (selectedSeats.size > ticketQuantity) {
                    val removed = selectedSeats.removeAt(selectedSeats.size - 1)
                    // Reset alpha for that button
                    when(removed) {
                        "A1" -> seatA1.alpha = 1.0f
                        "A2" -> seatA2.alpha = 1.0f
                        "A3" -> seatA3.alpha = 1.0f
                        "B1" -> seatB1.alpha = 1.0f
                        "B2" -> seatB2.alpha = 1.0f
                        "B3" -> seatB3.alpha = 1.0f
                    }
                }
                updateQuantityUI()

        val seatA1 = findViewById<Button>(R.id.seatA1)
        val seatA2 = findViewById<Button>(R.id.seatA2)
        val seatA3 = findViewById<Button>(R.id.seatA3)
        val seatB1 = findViewById<Button>(R.id.seatB1)
        val seatB2 = findViewById<Button>(R.id.seatB2)
        val seatB3 = findViewById<Button>(R.id.seatB3)
            }
        }

        val btnCheckout = findViewById<Button>(R.id.btnCheckout)
        val chipGroupShowtime = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupShowtime)

        setupSeatButton(seatA1, "A1") { ticketQuantity }
        setupSeatButton(seatA2, "A2") { ticketQuantity }
        setupSeatButton(seatA3, "A3") { ticketQuantity }
        setupSeatButton(seatB1, "B1") { ticketQuantity }
        setupSeatButton(seatB2, "B2") { ticketQuantity }
        setupSeatButton(seatB3, "B3") { ticketQuantity }

        setupBottomNavigation()

        btnCheckout.setOnClickListener {

            val selectedChipId = chipGroupShowtime.checkedChipId
            if (selectedChipId == android.view.View.NO_ID) {
                Toast.makeText(this, "Please select a showtime", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSeats.size != ticketQuantity) {
                Toast.makeText(
                    this,
                    "Please select exactly $ticketQuantity seat(s)",
                    Toast.LENGTH_SHORT,
                ).show()

            } else {
                val selectedChip = findViewById<com.google.android.material.chip.Chip>(selectedChipId)
                val selectedTime = selectedChip.text.toString()

                val intent = Intent(this, CheckoutActivity::class.java)

                intent.putExtra("movieTitle", movieTitle)
                intent.putExtra("movieGenre", movieGenre)
                intent.putExtra("showtime", selectedTime)
                intent.putStringArrayListExtra("selectedSeats", selectedSeats)

                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
    }

    private fun setupSeatButton(button: Button, seatNumber: String, getLimit: () -> Int) {

        button.setOnClickListener {

            if (selectedSeats.contains(seatNumber)) {

                selectedSeats.remove(seatNumber)
                button.alpha = 1.0f

            } else {
                if (selectedSeats.size < getLimit()) {
                    selectedSeats.add(seatNumber)
                    button.alpha = 0.5f
                } else {
                    Toast.makeText(this, "You can only select ${getLimit()} seat(s)", Toast.LENGTH_SHORT).show()
                }
            }

            textSelectedSeats.text =
                getString(R.string.selected_seats_label, selectedSeats.joinToString(", "))
        }
    }
}
