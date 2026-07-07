package com.fypcinemy

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object FirebaseMovieRepository {

    private const val COLLECTION_MOVIES = "movies"

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun listenNowShowing(
        onMoviesChanged: (List<Movie>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration {
        seedSandboxMovies()

        return db.collection(COLLECTION_MOVIES)
            .orderBy("displayOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    onMoviesChanged(MovieCatalog.nowShowing())
                    return@addSnapshotListener
                }

                val movies = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        val title = document.getString("title") ?: return@mapNotNull null
                        val genre = document.getString("genre") ?: return@mapNotNull null
                        val duration = document.getString("duration") ?: return@mapNotNull null
                        val description = document.getString("description")
                        val imageName = document.getString("imageName")

                        // Only include movies that are in our local MovieCatalog
                        val localMovie = MovieCatalog.findByTitle(title)
                        if (localMovie == null) {
                            android.util.Log.w("FirebaseMovieRepo", "Movie from Firestore not found in local catalog: $title")
                            return@mapNotNull null
                        }

                        Movie(
                            title = title,
                            genre = genre,
                            duration = duration,
                            imageResId = MovieCatalog.imageResIdFor(imageName, title),
                            imageName = imageName.orEmpty(),
                            description = description ?: localMovie.description
                        )
                    }
                    .orEmpty()

                onMoviesChanged(movies.ifEmpty { MovieCatalog.nowShowing() })
            }
    }

    private fun seedSandboxMovies() {
        MovieCatalog.nowShowing().forEachIndexed { index, movie ->
            val documentId = movie.title
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')

            val data = mapOf(
                "title" to movie.title,
                "genre" to movie.genre,
                "duration" to movie.duration,
                "imageName" to movie.imageName,
                "displayOrder" to index,
                "isNowShowing" to true,
            )

            db.collection(COLLECTION_MOVIES)
                .document(documentId)
                .set(data, SetOptions.merge())
        }
    }
}
