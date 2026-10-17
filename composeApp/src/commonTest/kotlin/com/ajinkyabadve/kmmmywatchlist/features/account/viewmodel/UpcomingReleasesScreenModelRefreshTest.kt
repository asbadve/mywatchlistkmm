package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingReleasesScreenModelRefreshTest {
    private val fakeTrackedMediaRepository = FakeTrackedMediaRepository()
    private val fakeTvDetailCacheRepository = FakeTvDetailCacheRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRefresh_syncsTheAccountThenTheLatestSeasonsOfEveryTrackedShow() {
        fakeTrackedMediaRepository.seedTrackedTvShows(
            listOf(
                TrackedTvShowSummary(id = FIRST_SHOW_ID, title = FIRST_SHOW_TITLE, posterPath = null),
                TrackedTvShowSummary(id = SECOND_SHOW_ID, title = SECOND_SHOW_TITLE, posterPath = null),
            ),
        )
        val viewModel =
            UpcomingReleasesScreenModel(
                fakeTrackedMediaRepository,
                fakeTvDetailCacheRepository,
                movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                regionRepository = FakeRegionRepository(),
            )

        viewModel.refresh(ACCOUNT_ID, SESSION_ID)

        assertEquals(listOf(ACCOUNT_ID to SESSION_ID), fakeTrackedMediaRepository.refreshAllCalls)
        assertEquals(
            setOf(FIRST_SHOW_ID.toLong(), SECOND_SHOW_ID.toLong()),
            fakeTvDetailCacheRepository.refreshLatestSeasonsCalls.toSet(),
        )
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun testRefresh_fetchesTheDetailsOfEveryUpcomingMovie() {
        fakeTrackedMediaRepository.seedUpcoming(
            listOf(
                UpcomingMediaItem(
                    id = MOVIE_ID,
                    mediaType = MediaTypeConstant.MOVIE,
                    title = MOVIE_TITLE,
                    posterPath = null,
                    date = MOVIE_DATE,
                    dateKind = UpcomingDateKind.MOVIE_RELEASE,
                ),
            ),
        )
        val movieDetailCacheRepository = FakeMovieDetailCacheRepository()
        val viewModel =
            UpcomingReleasesScreenModel(
                fakeTrackedMediaRepository,
                fakeTvDetailCacheRepository,
                movieDetailCacheRepository = movieDetailCacheRepository,
                regionRepository = FakeRegionRepository(),
            )

        viewModel.refresh(ACCOUNT_ID, SESSION_ID)

        assertEquals(listOf(MOVIE_ID.toLong()), movieDetailCacheRepository.getMovieDetailCalls)
    }

    private companion object {
        const val MOVIE_ID = 3
        const val MOVIE_TITLE = "The Mongoose"
        val MOVIE_DATE = LocalDate(2099, 1, 1)
        const val ACCOUNT_ID = 42L
        const val SESSION_ID = "session"
        const val FIRST_SHOW_ID = 1
        const val FIRST_SHOW_TITLE = "Slow Horses"
        const val SECOND_SHOW_ID = 2
        const val SECOND_SHOW_TITLE = "Severance"
    }
}
