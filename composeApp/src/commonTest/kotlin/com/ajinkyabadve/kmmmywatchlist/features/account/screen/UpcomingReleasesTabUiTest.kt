package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
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
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(FakeTrackedMediaRepository(), FakeTvDetailCacheRepository()),
                )
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
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                )
            }

            onAllNodesWithText("Upcoming Movie")[0].assertExists()
            onAllNodesWithText("Upcoming Show")[0].assertExists()
            onAllNodesWithText("S1 E5")[0].assertExists()
            // One month header per group: both items fall in different months.
            onNodeWithText("January 2099").assertExists()
            onNodeWithText("February 2099").assertExists()
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
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                )
            }

            // One "Lanterns" per row, plus one more in the Next-up spotlight card at the top.
            assertEquals(3, onAllNodesWithText("Lanterns").fetchSemanticsNodes().size)
        }

    @Test
    fun testTwoItemsSharingAMonth_renderOnlyOneHeaderForThatMonth() =
        runComposeUiTest {
            val sharedDate = LocalDate(2099, 3, 1)
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Same Month Movie",
                                posterPath = null,
                                date = sharedDate,
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 2, title = "Same Month Show", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 2,
                        season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-03-20"))),
                    )
                }

            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                )
            }

            onAllNodesWithText("Same Month Movie")[0].assertExists()
            onAllNodesWithText("Same Month Show")[0].assertExists()
            assertEquals(1, onAllNodesWithText("March 2099").fetchSemanticsNodes().size)
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
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, FakeTvDetailCacheRepository()),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = { selectedTvId = it },
                )
            }
            // Index 1: index 0 is the Next-up spotlight card showing the same title.
            onAllNodesWithText("Tappable Movie")[1].performClick()

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
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = { selectedTvId = it },
                )
            }
            // Index 1: index 0 is the Next-up spotlight card showing the same title.
            onAllNodesWithText("Tappable Show")[1].performClick()

            assertEquals(7L, selectedTvId)
            assertNull(selectedMovieId)
        }

    @Test
    fun testFilterChips_narrowTimelineToSelectedType() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Only Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = 2, title = "Only Show", posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCachedSeason(
                        tvId = 2,
                        season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-02-01"))),
                    )
                }

            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, fakeTvDetailCacheRepository),
                )
            }

            // "TV" also matches the MediaTypeBadge tag on the TV row itself, so the filter chip
            // (rendered first, above the timeline) is index 0.
            onAllNodesWithText("TV")[0].performClick()

            // "Only Movie" is the chronologically-earliest item, so it still shows once in the
            // Next-up spotlight card even though the TV filter hides it from the timeline below.
            assertEquals(1, onAllNodesWithText("Only Movie").fetchSemanticsNodes().size)
            assertEquals(1, onAllNodesWithText("Only Show").fetchSemanticsNodes().size)
        }

    @Test
    fun testExpandedWindowSize_rendersCalendarCard() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Expanded Layout Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }

            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.EXPANDED,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, FakeTvDetailCacheRepository()),
                )
            }

            onNodeWithText("Release").assertExists()
            onNodeWithText("Today").assertExists()
        }

    @Test
    fun testCompactWindowSize_doesNotRenderCalendarCard() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Compact Layout Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }

            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, FakeTvDetailCacheRepository()),
                )
            }

            onNodeWithText("Release").assertDoesNotExist()
        }

    @Test
    fun testDesktop_doesNotRenderReminderControls() =
        runComposeUiTest {
            // A local reminder notification has nothing to schedule against on desktop - the
            // not-yet-built reminder UI (item 16) must not render here, not even as a disabled
            // placeholder. This test runs as desktopTest, where isMobilePlatform() is false.
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = 1,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = "Bell Movie",
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }

            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.EXPANDED,
                    viewModel = UpcomingReleasesScreenModel(fakeTrackedMediaRepository, FakeTvDetailCacheRepository()),
                )
            }

            onNodeWithText("View details").assertExists()
            onNodeWithText("Remind me").assertDoesNotExist()
        }
}
