package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult

interface MovieRepository {
    suspend fun getMovies(pageNo: Int, moveFetchType: String): MoviePageResult
}
