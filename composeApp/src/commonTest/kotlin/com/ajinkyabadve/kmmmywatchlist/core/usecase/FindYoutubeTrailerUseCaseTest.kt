package com.ajinkyabadve.kmmmywatchlist.core.usecase

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindYoutubeTrailerUseCaseTest {
    private val findTrailer = FindYoutubeTrailerUseCase()

    private fun video(
        key: String,
        site: String = YOUTUBE_SITE,
        type: String = TRAILER_TYPE,
    ) = VideoResult(key = key, site = site, type = type)

    @Test
    fun testReturnsAWatchableUrlRatherThanABareKey() {
        val videos = VideoResponse(results = listOf(video(key = "abc123")))

        assertEquals(WATCH_URL + "abc123", findTrailer(videos))
    }

    /** Featurettes, clips and behind-the-scenes reels all share the videos array. */
    @Test
    fun testIgnoresVideosThatAreNotTrailers() {
        val videos =
            VideoResponse(
                results =
                    listOf(
                        video(key = "featurette", type = "Featurette"),
                        video(key = "realtrailer"),
                    ),
            )

        assertEquals(WATCH_URL + "realtrailer", findTrailer(videos))
    }

    /** The URL is built for YouTube specifically, so a Vimeo-hosted trailer is not playable here. */
    @Test
    fun testIgnoresTrailersHostedElsewhere() {
        val videos = VideoResponse(results = listOf(video(key = "vimeokey", site = "Vimeo")))

        assertNull(findTrailer(videos))
    }

    /** TMDB is inconsistent about casing on both fields, so matching has to tolerate it. */
    @Test
    fun testMatchesSiteAndTypeCaseInsensitively() {
        val videos =
            VideoResponse(
                results = listOf(video(key = "shouty", site = YOUTUBE_SITE.uppercase(), type = TRAILER_TYPE.lowercase())),
            )

        assertEquals(WATCH_URL + "shouty", findTrailer(videos))
    }

    /** The first trailer wins - TMDB returns them in the order it considers most relevant. */
    @Test
    fun testTakesTheFirstTrailerWhenThereAreSeveral() {
        val videos = VideoResponse(results = listOf(video(key = "first"), video(key = "second")))

        assertEquals(WATCH_URL + "first", findTrailer(videos))
    }

    /**
     * An empty key would build a URL to YouTube's front page, which is worse than showing no
     * trailer button at all.
     */
    @Test
    fun testTreatsAnEmptyKeyAsNoTrailer() {
        val videos = VideoResponse(results = listOf(video(key = "")))

        assertNull(findTrailer(videos))
    }

    @Test
    fun testReturnsNullWhenThereAreNoVideos() {
        assertNull(findTrailer(VideoResponse()))
    }

    /** The videos append is optional, so the whole object is absent on a partial detail response. */
    @Test
    fun testReturnsNullWhenTheVideosAppendIsMissing() {
        assertNull(findTrailer(null))
    }

    /**
     * Declared here rather than imported from the use case under test: these three values *are* the
     * contract - the site and type TMDB publishes and the link shape the app opens - and a test that
     * imported them would keep passing through any change to them.
     */
    private companion object {
        const val WATCH_URL = "https://www.youtube.com/watch?v="
        const val YOUTUBE_SITE = "YouTube"
        const val TRAILER_TYPE = "Trailer"
    }
}
