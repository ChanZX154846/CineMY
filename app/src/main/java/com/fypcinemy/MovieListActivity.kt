package com.fypcinemy

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration

class MovieListActivity : BaseActivity() {

    private lateinit var recyclerMovies: RecyclerView
    private lateinit var movieAdapter: MovieAdapter
    private var movieListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_movie_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerMovies = findViewById(R.id.recyclerMovies)
        recyclerMovies.layoutManager = LinearLayoutManager(this)

        movieAdapter = MovieAdapter(MovieCatalog.nowShowing())
        recyclerMovies.adapter = movieAdapter

        setupBottomNavigation()

        movieListener = FirebaseMovieRepository.listenNowShowing(
            onMoviesChanged = { movies -> movieAdapter.submitMovies(movies) },
            onError = {},
        )
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
