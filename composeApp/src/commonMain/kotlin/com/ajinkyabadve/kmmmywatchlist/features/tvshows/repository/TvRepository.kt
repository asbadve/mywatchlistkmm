package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail

interface TvRepository {
    suspend fun getTvShows(
        pageNo: Int,
        moveFetchType: String,
    ): TvPageResult

    suspend fun getTvDetails(tvId: Long): TvDetail

    suspend fun getSeasonDetails(
        tvId: Long,
        seasonNumber: Int,
    ): TvSeasonDetail

    suspend fun getEpisodeDetails(
        tvId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeDetail

    suspend fun getTvVideos(tvShowId: Long): VideoResponse
}
