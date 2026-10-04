package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
        val viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository)

        viewModel.refresh(ACCOUNT_ID, SESSION_ID)

        assertEquals(listOf(ACCOUNT_ID to SESSION_ID), fakeTrackedMediaRepository.refreshAllCalls)
        assertEquals(
            setOf(FIRST_SHOW_ID.toLong(), SECOND_SHOW_ID.toLong()),
            fakeTvDetailCacheRepository.refreshLatestSeasonsCalls.toSet(),
        )
        assertFalse(viewModel.isRefreshing.value)
    }

    private companion object {
        const val ACCOUNT_ID = 42L
        const val SESSION_ID = "session"
        const val FIRST_SHOW_ID = 1
        const val FIRST_SHOW_TITLE = "Slow Horses"
        const val SECOND_SHOW_ID = 2
        const val SECOND_SHOW_TITLE = "Severance"
    }
}
