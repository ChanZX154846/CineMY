package com.fypcinemy

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovieAdapter(movies: List<Movie>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val movieList = movies.toMutableList()

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgMovie: ImageView = itemView.findViewById(R.id.imgMovie)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtGenre: TextView = itemView.findViewById(R.id.txtGenre)
        val txtDuration: TextView = itemView.findViewById(R.id.txtDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)

        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {

        val movie = movieList[position]

        holder.txtTitle.text = movie.title
        holder.txtGenre.text = movie.genre
        holder.txtDuration.text = movie.duration
        holder.imgMovie.setImageResource(movie.imageResId)

        holder.itemView.setOnClickListener {

            val intent = Intent(holder.itemView.context, MovieDetailActivity::class.java)

            intent.putExtra(MovieDetailActivity.EXTRA_TITLE, movie.title)
            intent.putExtra(MovieDetailActivity.EXTRA_GENRE, movie.genre)
            intent.putExtra(MovieDetailActivity.EXTRA_DURATION, movie.duration)
            intent.putExtra(MovieDetailActivity.EXTRA_IMAGE, movie.imageResId)
            intent.putExtra(MovieDetailActivity.EXTRA_DESCRIPTION, movie.description)

            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    fun submitMovies(movies: List<Movie>) {
        movieList.clear()
        movieList.addAll(movies)
        notifyDataSetChanged()
    }
}
