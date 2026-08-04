package com.ajinkyabadve.kmmmywatchlist.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class TopLevelBackStackTest {
    @Test
    fun testSwitchingTabsWhileOnSearchDropsSearchFromTheDepartedTab() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(SearchKey)
        assertEquals(listOf(TrendingKey, SearchKey), backStack.backStack.toList())

        backStack.switchTopLevel(MoviesKey)
        assertEquals(listOf(TrendingKey, MoviesKey), backStack.backStack.toList())

        // Backing out of Movies must land on the Trending root, not resurrect search.
        backStack.removeLast()
        assertEquals(listOf(TrendingKey), backStack.backStack.toList())
        assertEquals(TrendingKey, backStack.topLevelKey)
    }

    @Test
    fun testDetailScreensAreStillRestoredWhenReturningToATab() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(MovieDetailKey(603))

        backStack.switchTopLevel(MoviesKey)
        // The detail is a real part of Trending's hierarchy, so it stays.
        assertEquals(listOf(TrendingKey, MovieDetailKey(603), MoviesKey), backStack.backStack.toList())

        backStack.removeLast()
        assertEquals(listOf(TrendingKey, MovieDetailKey(603)), backStack.backStack.toList())
    }

    @Test
    fun testSearchIsKeptWhenTheUserHasDrilledIntoAResult() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(SearchKey)
        backStack.add(MovieDetailKey(603))

        backStack.switchTopLevel(MoviesKey)

        // Only the *top* is transient - search survives underneath so that backing out of the
        // detail still returns to the results the user opened it from.
        assertEquals(
            listOf(TrendingKey, SearchKey, MovieDetailKey(603), MoviesKey),
            backStack.backStack.toList(),
        )

        backStack.removeLast()
        backStack.removeLast()
        assertEquals(listOf(TrendingKey, SearchKey), backStack.backStack.toList())
    }

    @Test
    fun testBackFromSearchRemovesItSoALaterTabSwitchIsUnaffected() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(SearchKey)

        backStack.removeLast()
        assertEquals(listOf(TrendingKey), backStack.backStack.toList())

        backStack.switchTopLevel(MoviesKey)
        backStack.removeLast()
        assertEquals(listOf(TrendingKey), backStack.backStack.toList())
    }

    @Test
    fun testReselectingTheCurrentTabAlsoDismissesSearch() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(SearchKey)

        backStack.switchTopLevel(TrendingKey)

        assertEquals(listOf(TrendingKey), backStack.backStack.toList())
    }

    @Test
    fun testEachTabKeepsItsOwnDepthAcrossSwitches() {
        val backStack = TopLevelBackStack(TrendingKey)
        backStack.add(MovieDetailKey(1))
        backStack.switchTopLevel(TvShowsKey)
        backStack.add(TvDetailKey(2))
        backStack.switchTopLevel(TrendingKey)

        assertEquals(
            listOf(TvShowsKey, TvDetailKey(2), TrendingKey, MovieDetailKey(1)),
            backStack.backStack.toList(),
        )
    }
}
