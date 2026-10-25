package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrailerOpenDetailsTest {
    @Test
    fun testMovieTrailer_opensMovieDetails() {
        var movieId: Long? = null
        var tvId: Long? = null

        trailer(isMovie = true).openDetails(onMovieSelected = { movieId = it }, onTvShowSelected = { tvId = it })

        assertEquals(MEDIA_ID, movieId)
        assertNull(tvId)
    }

    @Test
    fun testTvTrailer_opensTvDetails() {
        var movieId: Long? = null
        var tvId: Long? = null

        trailer(isMovie = false).openDetails(onMovieSelected = { movieId = it }, onTvShowSelected = { tvId = it })

        assertEquals(MEDIA_ID, tvId)
        assertNull(movieId)
    }

    private companion object {
        const val MEDIA_ID = 42L

        fun trailer(isMovie: Boolean) =
            Trailer(
                mediaId = MEDIA_ID,
                isMovie = isMovie,
                mediaTitle = "Title",
                backdropPath = null,
                video = VideoResult(id = "v", key = "k", site = "YouTube", type = "Trailer"),
            )
    }
}
