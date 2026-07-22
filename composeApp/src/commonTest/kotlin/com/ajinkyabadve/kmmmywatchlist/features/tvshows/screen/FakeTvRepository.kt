package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import io.ktor.utils.io.errors.IOException

class FakeTvRepository : TvRepository {
    var getTvShowsResult: Result<TvPageResult>? = null
    var getTvDetailsResult: Result<TvDetail>? = null
    var getSeasonDetailsResult: Result<TvSeasonDetail>? = null
    var getEpisodeDetailsResult: Result<EpisodeDetail>? = null

    /** Per-seasonNumber overrides, checked before [getSeasonDetailsResult]. */
    val getSeasonDetailsResultsByNumber = mutableMapOf<Int, Result<TvSeasonDetail>>()

    val getTvShowsCalls = mutableListOf<Pair<Int, String>>()
    val getTvDetailsCalls = mutableListOf<Long>()
    val getSeasonDetailsCalls = mutableListOf<Pair<Long, Int>>()
    val getEpisodeDetailsCalls = mutableListOf<Triple<Long, Int, Int>>()

    override suspend fun getTvShows(
        pageNo: Int,
        moveFetchType: String,
    ): TvPageResult {
        getTvShowsCalls.add(pageNo to moveFetchType)

        getTvShowsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        val tvShows =
            listOf(
                Tv(id = 201, title = "Tv Show A", originalTitle = "Tv Show A"),
            )
        return TvPageResult(
            page = 1,
            list = tvShows,
            totalResults = tvShows.size,
            totalPages = 1,
        )
    }

    override suspend fun getTvDetails(tvId: Long): TvDetail {
        getTvDetailsCalls.add(tvId)

        getTvDetailsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return TvDetail(
            id = tvId,
            title = "Fake Tv Detail",
            overview = "Overview of Fake Tv Detail",
            firstAirDate = "2026-07-02",
            voteAverage = 8.0,
            originalLanguage = "en",
        )
    }

    override suspend fun getSeasonDetails(
        tvId: Long,
        seasonNumber: Int,
    ): TvSeasonDetail {
        getSeasonDetailsCalls.add(tvId to seasonNumber)

        (getSeasonDetailsResultsByNumber[seasonNumber] ?: getSeasonDetailsResult)?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return TvSeasonDetail(
            id = tvId,
            seasonNumber = seasonNumber,
            name = "Season $seasonNumber",
            overview = "Overview of Season $seasonNumber",
        )
    }

    override suspend fun getEpisodeDetails(
        tvId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeDetail {
        getEpisodeDetailsCalls.add(Triple(tvId, seasonNumber, episodeNumber))

        getEpisodeDetailsResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return EpisodeDetail(
            id = tvId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            name = "Episode $episodeNumber",
            overview = "Overview of Episode $episodeNumber",
        )
    }
}
