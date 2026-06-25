package com.fypcinemy

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : BaseActivity() {

    private lateinit var movieAdapter: MovieAdapter
    private var movieListener: ListenerRegistration? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                checkRecommendations()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val recyclerMovies = findViewById<RecyclerView>(R.id.recyclerHomeMovies)
        movieAdapter = MovieAdapter(MovieCatalog.nowShowing())
        recyclerMovies.layoutManager = LinearLayoutManager(this)
        recyclerMovies.adapter = movieAdapter

        setupBottomNavigation()

        movieListener = FirebaseMovieRepository.listenNowShowing(
            onMoviesChanged = { movies -> movieAdapter.submitMovies(movies) },
            onError = {
                Toast.makeText(
                    this,
                    getString(R.string.firebase_movie_sync_error),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )

        if (RecommendationNotifier.hasNotificationPermission(this)) {
            checkRecommendations()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkRecommendations() {
        // Force recommendations to show for testing (disabling 12h cooldown)
        val tickets = TicketStore.getTickets(this)
        android.util.Log.d("HomeActivity", "Checking recommendations... Tickets: ${tickets.size}")
        RecommendationNotifier.showRecommendationsIfAvailable(this, tickets)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection()
    }

    override fun onDestroy() {
        movieListener?.remove()
        super.onDestroy()
    }
}
