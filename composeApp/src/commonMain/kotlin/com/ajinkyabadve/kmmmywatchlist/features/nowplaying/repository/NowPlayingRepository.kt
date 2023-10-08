package com.ajinkyabadve.kmmmywatchlist.features.nowplaying.repository

import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.model.MoviePageResult

interface NowPlayingRepository {
    suspend fun getNowPlayingMovies(): MoviePageResult
}
