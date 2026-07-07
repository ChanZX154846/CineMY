package com.fypcinemy

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

data class PurchasedTicket(
    val movieTitle: String,
    val genre: String?,
    val seats: List<String>,
    val totalPrice: Int,
    val bookingId: String,
    val purchasedAt: Long,
    val showtime: String? = null,
)

object TicketStore {

    private const val PREFS_NAME = "ticket_store"
    private const val KEY_TICKETS = "tickets"
    private const val COLLECTION_TICKETS = "tickets"

    fun addTicket(
        context: Context,
        movieTitle: String,
        genre: String?,
        seats: List<String>,
        totalPrice: Int,
        bookingId: String,
        showtime: String? = null,
    ) {
        val tickets = JSONArray(loadTicketsJson(context))
        val ticket = JSONObject()
            .put("movieTitle", movieTitle)
            .put("genre", genre)
            .put("seats", JSONArray(seats))
            .put("totalPrice", totalPrice)
            .put("bookingId", bookingId)
            .put("purchasedAt", System.currentTimeMillis())
            .put("showtime", showtime)

        tickets.put(ticket)
        saveTicketsJson(context, tickets.toString())
        saveTicketToFirebase(ticket)
    }

    fun getTickets(context: Context): List<PurchasedTicket> {
        val tickets = JSONArray(loadTicketsJson(context))
        val purchasedTickets = mutableListOf<PurchasedTicket>()

        for (index in 0 until tickets.length()) {
            val ticket = tickets.getJSONObject(index)
            val seatsJson = ticket.getJSONArray("seats")
            val seats = mutableListOf<String>()

            for (seatIndex in 0 until seatsJson.length()) {
                seats.add(seatsJson.getString(seatIndex))
            }

            purchasedTickets.add(
                PurchasedTicket(
                    movieTitle = ticket.getString("movieTitle"),
                    genre = ticket.optString("genre").takeIf { it.isNotBlank() },
                    seats = seats,
                    totalPrice = ticket.getInt("totalPrice"),
                    bookingId = ticket.getString("bookingId"),
                    purchasedAt = ticket.getLong("purchasedAt"),
                    showtime = ticket.optString("showtime").takeIf { it.isNotBlank() },
                ),
            )
        }

        return purchasedTickets.reversed()
    }

    fun listenTickets(
        context: Context,
        onTicketsChanged: (List<PurchasedTicket>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration? {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return null.also { onTicketsChanged(getTickets(context)) }

        return FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection(COLLECTION_TICKETS)
            .orderBy("purchasedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    onTicketsChanged(getTickets(context))
                    return@addSnapshotListener
                }

                val tickets = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        val seats = document.get("seats") as? List<*> ?: emptyList<String>()

                        PurchasedTicket(
                            movieTitle = document.getString("movieTitle") ?: return@mapNotNull null,
                            genre = document.getString("genre"),
                            seats = seats.mapNotNull { it as? String },
                            totalPrice = document.getLong("totalPrice")?.toInt() ?: 0,
                            bookingId = document.getString("bookingId") ?: document.id,
                            purchasedAt = document.getLong("purchasedAt") ?: 0L,
                            showtime = document.getString("showtime"),
                        )
                    }
                    .orEmpty()

                if (tickets.isNotEmpty()) {
                    saveTicketsJson(context, ticketsToJson(tickets))
                }

                onTicketsChanged(tickets.ifEmpty { getTickets(context) })
            }
    }

    private fun loadTicketsJson(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TICKETS, "[]") ?: "[]"
    }

    @SuppressLint("UseKtx")
    private fun saveTicketsJson(context: Context, ticketsJson: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TICKETS, ticketsJson)
            .apply()
    }

    private fun saveTicketToFirebase(ticket: JSONObject) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val bookingId = ticket.getString("bookingId")
        val seatsJson = ticket.getJSONArray("seats")
        val seats = mutableListOf<String>()

        for (index in 0 until seatsJson.length()) {
            seats.add(seatsJson.getString(index))
        }

        val data = mapOf(
            "movieTitle" to ticket.getString("movieTitle"),
            "genre" to ticket.optString("genre").takeIf { it.isNotBlank() },
            "seats" to seats,
            "totalPrice" to ticket.getInt("totalPrice"),
            "bookingId" to bookingId,
            "purchasedAt" to ticket.getLong("purchasedAt"),
            "showtime" to ticket.optString("showtime").takeIf { it.isNotBlank() },
            "userId" to user.uid,
            "userEmail" to user.email,
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection(COLLECTION_TICKETS)
            .document(bookingId)
            .set(data)

        db
            .collection(COLLECTION_TICKETS)
            .document(bookingId)
            .set(data)
    }

    private fun ticketsToJson(tickets: List<PurchasedTicket>): String {
        val ticketsJson = JSONArray()

        tickets.reversed().forEach { ticket ->
            ticketsJson.put(
                JSONObject()
                    .put("movieTitle", ticket.movieTitle)
                    .put("genre", ticket.genre)
                    .put("seats", JSONArray(ticket.seats))
                    .put("totalPrice", ticket.totalPrice)
                    .put("bookingId", ticket.bookingId)
                    .put("purchasedAt", ticket.purchasedAt)
                    .put("showtime", ticket.showtime),
            )
        }

        return ticketsJson.toString()
    }
}
