package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre

class FakeGenreRepository(
    var movieGenres: List<Genre> = emptyList(),
    var tvGenres: List<Genre> = emptyList(),
) : GenreRepository {
    override suspend fun getMovieGenres(): List<Genre> = movieGenres

    override suspend fun getTvGenres(): List<Genre> = tvGenres
}
