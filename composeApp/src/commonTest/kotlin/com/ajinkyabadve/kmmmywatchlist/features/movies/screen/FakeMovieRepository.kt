package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import io.ktor.utils.io.errors.IOException

class FakeMovieRepository : MovieRepository {
    var getMoviesResult: Result<MoviePageResult>? = null

    val getMoviesCalls = mutableListOf<Pair<Int, String>>()

    override suspend fun getMovies(pageNo: Int, moveFetchType: String): MoviePageResult {
        getMoviesCalls.add(pageNo to moveFetchType)
        
        getMoviesResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        val movies = when (moveFetchType) {
            "now_playing" -> listOf(Movie(id = 101, title = "Now Playing Movie A"))
            "upcoming" -> listOf(Movie(id = 102, title = "Upcoming Movie B"))
            "popular" -> listOf(Movie(id = 103, title = "Popular Movie C"))
            "top_rated" -> listOf(Movie(id = 104, title = "Top Rated Movie D"))
            else -> emptyList()
        }
        return MoviePageResult(
            page = 1,
            list = movies,
            totalResults = movies.size,
            totalPages = 1
        )
    }
}
