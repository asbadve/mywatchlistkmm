package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrailerCacheRepositoryImplTest {
    private val settings = FakeSettings()
    private var now = SAVED_AT

    private val repository = TrailerCacheRepositoryImpl(settings = settings, nowMillis = { now })

    private val trailers =
        listOf(
            Trailer(
                mediaId = MEDIA_ID,
                isMovie = true,
                mediaTitle = TITLE,
                backdropPath = BACKDROP,
                video =
                    VideoResult(
                        id = VIDEO_ID,
                        key = VIDEO_KEY,
                        name = VIDEO_NAME,
                        site = SITE,
                        type = TYPE,
                        publishedAt = PUBLISHED_AT,
                    ),
            ),
        )

    @Test
    fun testPutThenGet_returnsTheSameTrailersWhileFresh() {
        repository.put(TrailerSource.IN_THEATERS, trailers)
        now = SAVED_AT + TrailerCacheConstant.TTL_MILLIS - 1

        assertEquals(trailers, repository.get(TrailerSource.IN_THEATERS))
    }

    @Test
    fun testGet_afterTwelveHoursIsStale() {
        repository.put(TrailerSource.IN_THEATERS, trailers)
        now = SAVED_AT + TrailerCacheConstant.TTL_MILLIS

        assertNull(repository.get(TrailerSource.IN_THEATERS))
    }

    @Test
    fun testGet_whenTheClockWentBackwardsIsStale() {
        repository.put(TrailerSource.IN_THEATERS, trailers)
        now = SAVED_AT - 1

        assertNull(repository.get(TrailerSource.IN_THEATERS))
    }

    @Test
    fun testSources_areCachedSeparately() {
        repository.put(TrailerSource.IN_THEATERS, trailers)

        assertNull(repository.get(TrailerSource.ON_TV))
    }

    @Test
    fun testCorruptData_readsAsNoCache() {
        settings.putString(TrailerCacheConstant.KEY_PREFIX + TrailerSource.UPCOMING.name.lowercase(), CORRUPT_JSON)

        assertNull(repository.get(TrailerSource.UPCOMING))
    }

    private companion object {
        const val SAVED_AT = 1_791_000_000_000L
        const val MEDIA_ID = 45L
        const val TITLE = "Sintel"
        const val BACKDROP = "/backdrop.jpg"
        const val VIDEO_ID = "video-1"
        const val VIDEO_KEY = "eRsGyueVLvQ"
        const val VIDEO_NAME = "Official Trailer"
        const val SITE = "YouTube"
        const val TYPE = "Trailer"
        const val PUBLISHED_AT = "2026-01-01T00:00:00.000Z"
        const val CORRUPT_JSON = "{not json"
    }
}
