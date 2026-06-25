package com.fypcinemy

data class Movie(
    val title: String,
    val genre: String,
    val duration: String,
    val imageResId: Int,
    val imageName: String = "",
    val description: String = "",
)
