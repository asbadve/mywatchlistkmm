package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult

interface MovieRepository {
    suspend fun getMovies(pageNo: Int, moveFetchType: String): MoviePageResult
    suspend fun getMovieDetails(movieId: Long): MovieDetail
    suspend fun getCollectionDetails(collectionId: Long): CollectionDetail
    suspend fun getMovieCredits(movieId: Long): Credits
}
