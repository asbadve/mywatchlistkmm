package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult

class FakeDiscoverRepository : DiscoverRepository {
    var discoverMoviesResult: Result<MoviePageResult> = Result.failure(IllegalStateException("not stubbed"))
    var discoverTvShowsResult: Result<TvPageResult> = Result.failure(IllegalStateException("not stubbed"))
    val discoverMoviesCalls = mutableListOf<Triple<Int, DiscoverFilters, Boolean>>()
    val discoverTvShowsCalls = mutableListOf<Triple<Int, DiscoverFilters, Boolean>>()

    override suspend fun getDiscoverMovies(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): MoviePageResult {
        discoverMoviesCalls.add(Triple(pageNo, filters, includeAdult))
        return discoverMoviesResult.getOrThrow()
    }

    override suspend fun getDiscoverTvShows(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): TvPageResult {
        discoverTvShowsCalls.add(Triple(pageNo, filters, includeAdult))
        return discoverTvShowsResult.getOrThrow()
    }
}
