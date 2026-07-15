package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository

class FakeTvRepository : TvRepository {
    override suspend fun getTvShows(pageNo: Int, moveFetchType: String): TvPageResult {
        val tvShows = listOf(
            Tv(id = 201, title = "Tv Show A", originalTitle = "Tv Show A")
        )
        return TvPageResult(
            page = 1,
            list = tvShows,
            totalResults = tvShows.size,
            totalPages = 1
        )
    }

    override suspend fun getTvDetails(tvId: Long): TvDetail {
        return TvDetail(
            id = tvId,
            title = "Fake Tv Detail",
            overview = "Overview of Fake Tv Detail",
            firstAirDate = "2026-07-02",
            voteAverage = 8.0,
            originalLanguage = "en",
        )
    }

    override suspend fun getSeasonDetails(tvId: Long, seasonNumber: Int): TvSeasonDetail {
        return TvSeasonDetail(
            id = tvId,
            seasonNumber = seasonNumber,
            name = "Season $seasonNumber",
            overview = "Overview of Season $seasonNumber",
        )
    }
}
