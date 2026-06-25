package com.fypcinemy

object MovieCatalog {

    fun nowShowing(): List<Movie> {
        return listOf(
            Movie(
                "Shang-Chi",
                "Action",
                "2h 12m",
                R.drawable.shangchi,
                "shangchi",
                "Martial-arts master Shang-Chi confronts the past he thought he left behind when he's drawn into the web of the mysterious Ten Rings organization."
            ),
            Movie(
                "Superman",
                "Action / Sci-Fi",
                "2h 9m",
                R.drawable.superman,
                "superman",
                "Superman, a reporter in Metropolis, embarks on a journey to reconcile his Kryptonian heritage with his human upbringing as Clark Kent."
            ),
            Movie(
                "Jurassic World Rebirth",
                "Action / Sci-Fi",
                "2h 13m",
                R.drawable.jurassic,
                "jurassic",
                "Five years after the events of Jurassic World Dominion, the planet's ecology has proven largely inhospitable to dinosaurs. Those remaining exist in isolated equatorial environments."
            ),
            Movie(
                "The Batman",
                "Action / Horror",
                "2h 56m",
                R.drawable.thebatman,
                "thebatman",
                "Batman ventures into Gotham City's underworld when a sadistic killer leaves behind a trail of cryptic clues. As the evidence begins to lead closer to home and the scale of the perpetrator's plans becomes clear, he must forge new relationships, unmask the culprit and bring justice to the abuse of power and corruption that has long plagued the metropolis."
            ),
        )
    }

    fun imageResIdFor(imageName: String?, title: String): Int {
        return when (imageName?.lowercase()?.trim()) {
            "shangchi", "shang-chi" -> R.drawable.shangchi
            "superman" -> R.drawable.superman
            "jurassic", "jurassic_world_rebirth" -> R.drawable.jurassic
            "thebatman", "the_batman" -> R.drawable.thebatman
            else -> findByTitle(title)?.imageResId ?: R.drawable.shangchi
        }
    }

    fun findByTitle(title: String): Movie? {
        return nowShowing().firstOrNull { movie ->
            movie.title.equals(title, ignoreCase = true)
        }
    }

    fun findRecommendationsFor(tickets: List<PurchasedTicket>): List<Movie> {
        // Only consider the top 4 movies as requested
        val top4Movies = nowShowing().take(4)
        
        // If there are no tickets yet (new user), just recommend 1 random from the top 4
        if (tickets.isEmpty()) {
            return top4Movies.shuffled().take(1)
        }

        // Get the title of the last movie booked to exclude it from the recommendation
        val lastBookedTitle = tickets.first().movieTitle

        // Filter out the last booked movie from the top 4 to recommend one of the "other 3"
        // We DON'T filter out other previously purchased movies as per request ("Don't care if it's already purchased")
        val other3Movies = top4Movies.filter { !it.title.equals(lastBookedTitle, ignoreCase = true) }

        // If for some reason lastBookedTitle wasn't in top 4, other3Movies might have 4 items, 
        // but we still just take 1 random from whatever is available in top 4 except the last one.
        return other3Movies.shuffled().take(1)
    }

    private fun splitGenres(genre: String?): List<String> {
        return genre
            ?.split("/", ",", "&")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    }
}
