package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class UpcomingReleasesTabUiTest {
    @Test
    fun testEmptyState_showsWhenNothingUpcoming() =
        runComposeUiTest {
            setContent {
                UpcomingReleasesTab(viewModel = UpcomingReleasesScreenModel(FakeTrackedMediaRepository(), FakeTvDetailCacheRepository()))
            }
            onNodeWithText("Nothing upcoming").assertExists()
        }

    @Test
    fun testUpcomingMovieAndEpisode_bothRender() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Upcoming Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 2, title = "Upcoming Show", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 2,
                        season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 5, airDate = "2099-02-01"))),
                    )
                }

            setContent {
                UpcomingReleasesTab(viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository))
            }

            onAllNodesWithText("Upcoming Movie")[0].assertExists()
            onAllNodesWithText("Upcoming Show")[0].assertExists()
            onNodeWithText("S1 E5").assertExists()
            // One date header per group: both items' distinct dates each get their own header.
            onNodeWithText(LocalDate(2099, 1, 1).toString()).assertExists()
            onNodeWithText(LocalDate(2099, 2, 1).toString()).assertExists()
        }

    @Test
    fun testShowWithMultipleUnreleasedEpisodes_rendersOneRowPerEpisode() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 1, title = "Lanterns", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 1,
                        season =
                            TvSeasonDetail(
                                seasonNumber = 2,
                                episodes =
                                    listOf(
                                        Episode(episodeNumber = 5, airDate = "2099-01-01"),
                                        Episode(episodeNumber = 6, airDate = "2099-01-08"),
                                    ),
                            ),
                    )
                }

            setContent {
                UpcomingReleasesTab(viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository))
            }

            assertEquals(2, onAllNodesWithText("Lanterns").fetchSemanticsNodes().size)
        }

    @Test
    fun testTwoItemsSharingADate_renderOnlyOneHeaderForThatDate() =
        runComposeUiTest {
            val sharedDate = LocalDate(2099, 3, 1)
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Same Day Movie",
                                posterPath = null,
                                date = sharedDate,
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 2, title = "Same Day Show", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 2,
                        season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-03-01"))),
                    )
                }

            setContent {
                UpcomingReleasesTab(viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository))
            }

            onAllNodesWithText("Same Day Movie")[0].assertExists()
            onAllNodesWithText("Same Day Show")[0].assertExists()
            assertEquals(1, onAllNodesWithText(sharedDate.toString()).fetchSemanticsNodes().size)
        }

    @Test
    fun testTappingMovieRow_invokesOnMovieSelected() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 42,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Tappable Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }
            var selectedMovieId: Long? = null
            var selectedTvId: Long? = null

            setContent {
                UpcomingReleasesTab(
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, FakeTvDetailCacheRepository()),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = { selectedTvId = it },
                )
            }
            onAllNodesWithText("Tappable Movie")[0].performClick()

            assertEquals(42L, selectedMovieId)
            assertNull(selectedTvId)
        }

    @Test
    fun testTappingEpisodeRow_invokesOnTvSelected() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 7, title = "Tappable Show", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 7,
                        season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-01-01"))),
                    )
                }
            var selectedMovieId: Long? = null
            var selectedTvId: Long? = null

            setContent {
                UpcomingReleasesTab(
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = { selectedTvId = it },
                )
            }
            onAllNodesWithText("Tappable Show")[0].performClick()

            assertEquals(7L, selectedTvId)
            assertNull(selectedMovieId)
        }
}
