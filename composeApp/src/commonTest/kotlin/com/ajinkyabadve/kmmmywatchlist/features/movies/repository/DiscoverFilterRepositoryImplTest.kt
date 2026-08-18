package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoverFilterRepositoryImplTest {
    @Test
    fun testDefaultMovieFiltersAreLastYearPopularityDescendingWhenNothingPersisted() {
        val repository = DiscoverFilterRepositoryImpl(FakeSettings())

        val filters = repository.getSelectedMovieFilters()

        val lastYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year - 1
        assertEquals(lastYear, filters.year)
        assertEquals("popularity.desc", filters.sortBy)
        assertTrue(filters.genreIds.isEmpty())
        assertTrue(filters.keywords.isEmpty())
    }

    @Test
    fun testSetThenGetMovieFiltersRoundTrips() {
        val repository = DiscoverFilterRepositoryImpl(FakeSettings())
        val applied =
            DiscoverFilters(
                genreIds = setOf(28, 12),
                keywords = listOf(Keyword(id = 9715, name = "superhero")),
                year = 2020,
                sortBy = "vote_average.desc",
            )

        repository.setSelectedMovieFilters(applied)

        assertEquals(applied, repository.getSelectedMovieFilters())
    }

    @Test
    fun testMovieAndTvFiltersArePersistedIndependently() {
        val repository = DiscoverFilterRepositoryImpl(FakeSettings())
        val movieFilters = DiscoverFilters(genreIds = setOf(28))
        val tvFilters = DiscoverFilters(genreIds = setOf(10759))

        repository.setSelectedMovieFilters(movieFilters)
        repository.setSelectedTvFilters(tvFilters)

        assertEquals(movieFilters, repository.getSelectedMovieFilters())
        assertEquals(tvFilters, repository.getSelectedTvFilters())
    }
}
