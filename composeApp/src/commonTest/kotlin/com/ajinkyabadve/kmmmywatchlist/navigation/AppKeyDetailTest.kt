package com.ajinkyabadve.kmmmywatchlist.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [isDetailKey] decides two things at once in App.kt - whether the app's own top bar is suppressed
 * and whether the bottom nav bar collapses on scroll - so a key landing in the wrong bucket shows
 * up as a doubled toolbar or a nav bar that will not get out of the way.
 */
class AppKeyDetailTest {
    @Test
    fun testEveryDrillDownDestinationIsADetailKey() {
        val detailKeys =
            listOf(
                MovieDetailKey(movieId = 1),
                CollectionDetailKey(collectionId = 1),
                TvDetailKey(tvShowId = 1),
                PersonDetailKey(personId = 1),
                AllSeasonsKey(tvShowId = 1),
                EpisodeListKey(tvShowId = 1, seasonNumber = 1),
                EpisodeDetailKey(tvShowId = 1, seasonNumber = 1, episodeNumber = 1),
            )

        detailKeys.forEach { key ->
            assertTrue(key.isDetailKey(), "$key should be a detail key")
        }
    }

    @Test
    fun testBrowseTabsAndSearchAreNotDetailKeys() {
        val nonDetailKeys = listOf(TrendingKey, MoviesKey, TvShowsKey, PersonKey, MyFavKey, SearchKey)

        nonDetailKeys.forEach { key ->
            assertFalse(key.isDetailKey(), "$key should not be a detail key")
        }
    }

    /** currentKey is nullable at the App.kt call site while the back stack settles. */
    @Test
    fun testNullKeyIsNotADetailKey() {
        assertFalse((null as AppKey?).isDetailKey())
    }
}
