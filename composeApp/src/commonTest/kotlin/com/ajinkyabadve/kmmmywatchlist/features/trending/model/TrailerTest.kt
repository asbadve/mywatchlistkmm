package com.ajinkyabadve.kmmmywatchlist.features.trending.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrailerTest {
    @Test
    fun picksOfficialTrailerOverTeaserAndNonYoutube() {
        val videos =
            listOf(
                VideoResult(
                    id = "1",
                    key = "abc",
                    site = "YouTube",
                    type = "Teaser",
                    official = true,
                    publishedAt = "2026-01-01T00:00:00.000Z",
                ),
                VideoResult(
                    id = "2",
                    key = "def",
                    site = "YouTube",
                    type = "Trailer",
                    official = true,
                    publishedAt = "2026-01-02T00:00:00.000Z",
                ),
                VideoResult(
                    id = "3",
                    key = "",
                    site = "Vimeo",
                    type = "Trailer",
                    official = true,
                    publishedAt = "2026-01-03T00:00:00.000Z",
                ),
            )

        assertEquals("2", latestTrailerVideo(videos)?.id)
    }

    @Test
    fun prefersOfficialOverUnofficialTrailer() {
        val videos =
            listOf(
                VideoResult(
                    id = "unofficial",
                    key = "u",
                    site = "YouTube",
                    type = "Trailer",
                    official = false,
                    publishedAt = "2026-01-05T00:00:00.000Z",
                ),
                VideoResult(
                    id = "official",
                    key = "o",
                    site = "YouTube",
                    type = "Trailer",
                    official = true,
                    publishedAt = "2026-01-01T00:00:00.000Z",
                ),
            )

        assertEquals("official", latestTrailerVideo(videos)?.id)
    }

    @Test
    fun prefersNewestWhenTypeAndOfficialTie() {
        val videos =
            listOf(
                VideoResult(
                    id = "older",
                    key = "a",
                    site = "YouTube",
                    type = "Trailer",
                    official = true,
                    publishedAt = "2025-01-01T00:00:00.000Z",
                ),
                VideoResult(
                    id = "newer",
                    key = "b",
                    site = "YouTube",
                    type = "Trailer",
                    official = true,
                    publishedAt = "2026-01-01T00:00:00.000Z",
                ),
            )

        assertEquals("newer", latestTrailerVideo(videos)?.id)
    }

    @Test
    fun fallsBackToTeaserWhenNoTrailerExists() {
        val videos =
            listOf(
                VideoResult(id = "featurette", key = "f", site = "YouTube", type = "Featurette", official = true),
                VideoResult(id = "teaser", key = "t", site = "YouTube", type = "Teaser", official = true),
            )

        assertEquals("teaser", latestTrailerVideo(videos)?.id)
    }

    @Test
    fun returnsNullWhenNothingPlayableExists() {
        val videos =
            listOf(
                VideoResult(id = "1", key = "x", site = "YouTube", type = "Featurette", official = true),
                VideoResult(id = "2", key = "", site = "YouTube", type = "Trailer", official = true),
            )

        assertNull(latestTrailerVideo(videos))
        assertNull(latestTrailerVideo(emptyList()))
    }

    @Test
    fun buildsYoutubeWatchUrlFromKey() {
        val video = VideoResult(key = "eO0pgPxeNGc")
        assertEquals("https://www.youtube.com/watch?v=eO0pgPxeNGc", video.youtubeUrl())
    }
}
