package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import io.ktor.utils.io.errors.IOException

class FakeMovieRepository : MovieRepository {
    var getMoviesResult: Result<MoviePageResult>? = null
    var getMovieDetailsResult: Result<MovieDetail>? = null
    var getCollectionDetailsResult: Result<CollectionDetail>? = null

    // Per-movie credits for the collection featured cast/crew aggregation.
    val getMovieCreditsResults = mutableMapOf<Long, Result<Credits>>()
    val getMovieCreditsCalls = mutableListOf<Long>()
    val getCollectionDetailsCalls = mutableListOf<Long>()

    // Per-movie videos for the Latest Trailers rail.
    val getMovieVideosResults = mutableMapOf<Long, Result<VideoResponse>>()
    val getMovieVideosCalls = mutableListOf<Long>()

    val getMoviesCalls = mutableListOf<Pair<Int, String>>()
    val getMovieDetailsCalls = mutableListOf<Long>()

    override suspend fun getMovies(
        pageNo: Int,
        moveFetchType: String,
    ): MoviePageResult {
        getMoviesCalls.add(pageNo to moveFetchType)

        getMoviesResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        val movies =
            when (moveFetchType) {
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
            totalPages = 1,
        )
    }

    override suspend fun getMovieDetails(movieId: Long): MovieDetail {
        getMovieDetailsCalls.add(movieId)

        getMovieDetailsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return MovieDetail(
            id = movieId,
            title = "Fake Movie Detail",
            overview = "Overview of Fake Movie Detail",
            genres = emptyList(),
            keywords = null,
            videos = null,
            images = null,
            credits = null,
            releaseDates = null,
            translations = null,
            runtime = 120,
            budget = 10000000L,
            voteAverage = 8.0,
            originalLanguage = "en",
            releaseDate = "2026-07-02",
            backdropPath = null,
            posterPath = null,
            recommendations = null,
            similar = null,
            reviews = null,
        )
    }

    override suspend fun getCollectionDetails(collectionId: Long): CollectionDetail {
        getCollectionDetailsCalls.add(collectionId)

        getCollectionDetailsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return CollectionDetail(id = collectionId, name = "Collection A")
    }

    override suspend fun getMovieCredits(movieId: Long): Credits {
        getMovieCreditsCalls.add(movieId)

        getMovieCreditsResults[movieId]?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return Credits()
    }

    override suspend fun getMovieVideos(movieId: Long): VideoResponse {
        getMovieVideosCalls.add(movieId)

        getMovieVideosResults[movieId]?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return VideoResponse()
    }
}
