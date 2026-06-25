package com.fypcinemy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MovieDetailActivity : BaseActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_GENRE = "genre"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_IMAGE = "image"
        const val EXTRA_DESCRIPTION = "description"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_movie_detail)

        val imgMovie = findViewById<ImageView>(R.id.imgMovieDetail)
        val txtTitle = findViewById<TextView>(R.id.txtMovieTitle)
        val txtGenre = findViewById<TextView>(R.id.txtMovieGenre)
        val txtDuration = findViewById<TextView>(R.id.txtMovieDuration)
        val txtDescription = findViewById<TextView>(R.id.txtMovieDescription)

        val btnBook = findViewById<Button>(R.id.btnBookTicket)
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val genre = intent.getStringExtra(EXTRA_GENRE).orEmpty()
        val duration = intent.getStringExtra(EXTRA_DURATION).orEmpty()
        val image = intent.getIntExtra(EXTRA_IMAGE, 0)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()

        txtTitle.text = title
        txtGenre.text = genre
        txtDuration.text = duration

        if (image != 0) {
            imgMovie.setImageResource(image)
        }

        txtDescription.text = description.ifEmpty { getString(R.string.immersive_experience_desc) }

        setupBottomNavigation()

        btnBook.setOnClickListener {

            startActivity(
                Intent(this, SeatSelectionActivity::class.java)
                    .putExtra(SeatSelectionActivity.EXTRA_MOVIE_TITLE, title)
                    .putExtra(SeatSelectionActivity.EXTRA_MOVIE_GENRE, genre),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
    }
}
