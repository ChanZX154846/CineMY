package com.fypcinemy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

object RecommendationNotifier {

    const val REQUEST_NOTIFICATIONS = 2101

    private const val CHANNEL_ID = "movie_recommendations"
    private const val NOTIFICATION_ID_BASE = 3000

    fun showRecommendationsIfAvailable(context: Context, tickets: List<PurchasedTicket>) {
        Log.d("RecommendationNotifier", "Checking recommendations... Tickets size: ${tickets.size}")
        
        // Ensure channel is created first
        createChannel(context)

        // Settings check
        val recommendationsEnabled = context
            .getSharedPreferences("cinemy_settings", Context.MODE_PRIVATE)
            .getBoolean("movie_recommendations_enabled", true)

        if (!recommendationsEnabled) {
            Log.d("RecommendationNotifier", "Recommendations disabled in settings")
            return
        }

        if (!hasNotificationPermission(context)) {
            Log.d("RecommendationNotifier", "No notification permission")
            return
        }

        val recommendedMovies = MovieCatalog.findRecommendationsFor(tickets)
        Log.d("RecommendationNotifier", "Recommended movies found: ${recommendedMovies.size}")
        if (recommendedMovies.isEmpty()) {
            Log.d("RecommendationNotifier", "No recommendations to show")
            return
        }

        // Add a Toast for visible feedback during testing/debug
        Toast.makeText(context, "Showing ${recommendedMovies.size} recommendations", Toast.LENGTH_SHORT).show()

        val notificationManager = NotificationManagerCompat.from(context)

        recommendedMovies.forEachIndexed { index, movie ->
            // Use Math.abs to ensure positive notification ID
            val notificationId = NOTIFICATION_ID_BASE + Math.abs(movie.title.hashCode())
            
            val isGenreBased = tickets.isNotEmpty() && 
                MovieCatalog.findByTitle(movie.title)?.let { m ->
                    val userGenres = tickets.flatMap { t -> 
                        t.genre?.split("/", ",", "&")?.map { it.trim().lowercase() } ?: emptyList()
                    }
                    m.genre.split("/", ",", "&").map { it.trim().lowercase() }.any { it in userGenres }
                } ?: false

            val message = if (isGenreBased) {
                context.getString(R.string.recommendation_notification_message, movie.title, movie.genre)
            } else {
                context.getString(R.string.recommendation_notification_generic, movie.title)
            }

            val intent = Intent(context, MovieDetailActivity::class.java)
                .putExtra(MovieDetailActivity.EXTRA_TITLE, movie.title)
                .putExtra(MovieDetailActivity.EXTRA_GENRE, movie.genre)
                .putExtra(MovieDetailActivity.EXTRA_DURATION, movie.duration)
                .putExtra(MovieDetailActivity.EXTRA_IMAGE, movie.imageResId)
                .putExtra(MovieDetailActivity.EXTRA_DESCRIPTION, movie.description)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(
                context,
                Math.abs(movie.title.hashCode()), // Unique positive request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.recommendation_notification_title))
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH for heads-up
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, false) // Try to force heads-up
                .build()

            try {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, notification)
                    Log.d("RecommendationNotifier", "Notification sent for: ${movie.title} with ID: $notificationId")
                } else {
                    Log.d("RecommendationNotifier", "No permission to post notifications")
                }
            } catch (e: SecurityException) {
                Log.e("RecommendationNotifier", "Permission lost", e)
            }
        }

        // Maintain cooldown in SharedPreferences (Optional: comment out for testing)
        context.getSharedPreferences("cinemy_recommendations", Context.MODE_PRIVATE).edit {
            putLong("last_recommendation_time", System.currentTimeMillis())
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            
        Log.d("RecommendationNotifier", "Permission check: systemEnabled=$systemNotificationsEnabled, manifestGranted=$permissionGranted")
        return systemNotificationsEnabled && permissionGranted
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.recommendation_channel_name),
            NotificationManager.IMPORTANCE_HIGH, // HIGH for head-up notifications
        ).apply {
            description = context.getString(R.string.recommendation_channel_description)
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }
}
