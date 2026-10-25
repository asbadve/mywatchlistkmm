package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.LocalNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel.UpcomingReleasesScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.model.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Network
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UpcomingReleasesTabUiTest {
    @Test
    fun testEmptyState_showsWhenNothingUpcoming() =
        runComposeUiTest {
            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel =
                        UpcomingReleasesScreenModel(
                            FakeTrackedMediaRepository(),
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
                )
            }

            onNodeWithText("Release").assertDoesNotExist()
        }

    @Test
    fun testRemindersUnavailable_doesNotRenderReminderControls() =
        runComposeUiTest {
            // Desktop and web have nothing to schedule a reminder against, so the reminder UI must
            // not render there, not even as a disabled placeholder. Passed explicitly rather than
            // relying on isMobilePlatform(): this suite also runs on the iOS simulator.
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
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
                    showReminderControls = false,
                )
            }

            onNodeWithText("View details").assertExists()
            onNodeWithText("Remind me").assertDoesNotExist()
        }

    @Test
    fun testTappingABell_setsAReminderForThatRow() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = BELL_MOVIE_ID,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = BELL_MOVIE_TITLE,
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }
            val releaseReminderRepository = FakeReleaseReminderRepository()
            val viewModel =
                UpcomingReleasesScreenModel(
                    trackedMediaRepository = fakeTrackedMediaRepository,
                    tvDetailCacheRepository = FakeTvDetailCacheRepository(),
                    releaseReminderRepository = releaseReminderRepository,
                    releaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository, FakeReminderScheduler()),
                    // No-op job hooks: the real ones would start desktop's background poll loop.
                    notificationJobSync =
                        NotificationJobSync(
                            notificationSettingsRepository = FakeNotificationSettingsRepository(),
                            releaseReminderRepository = releaseReminderRepository,
                            scheduleJob = {},
                            cancelJob = {},
                        ),
                    movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                    regionRepository = FakeRegionRepository(),
                )

            setContent {
                CompositionLocalProvider(LocalNotificationPermissionRequester provides FakeNotificationPermissionRequester()) {
                    UpcomingReleasesTab(windowSize = WindowSize.COMPACT, viewModel = viewModel, showReminderControls = true)
                }
            }

            // The compact Next-up card and the timeline row both carry a bell for this title.
            onAllNodesWithContentDescription(REMIND_ME_DESCRIPTION)[0].performClick()
            waitUntil { releaseReminderRepository.setReminderCalls.isNotEmpty() }

            assertEquals(
                listOf(ReminderKey(BELL_MOVIE_ID.toLong(), MediaTypeConstant.MOVIE) to true),
                releaseReminderRepository.setReminderCalls,
            )
            waitUntil { onAllNodesWithContentDescription(REMINDER_SET_DESCRIPTION).fetchSemanticsNodes().isNotEmpty() }
        }

    @Test
    fun testCompactPhoneWidth_titlesKeepAReadableWidthWithBellsShown() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = BELL_MOVIE_ID,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = LONG_TITLE,
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }
            val releaseReminderRepository = FakeReleaseReminderRepository()
            val viewModel =
                UpcomingReleasesScreenModel(
                    trackedMediaRepository = fakeTrackedMediaRepository,
                    tvDetailCacheRepository = FakeTvDetailCacheRepository(),
                    releaseReminderRepository = releaseReminderRepository,
                    releaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository, FakeReminderScheduler()),
                    notificationJobSync =
                        NotificationJobSync(
                            notificationSettingsRepository = FakeNotificationSettingsRepository(),
                            releaseReminderRepository = releaseReminderRepository,
                            scheduleJob = {},
                            cancelJob = {},
                        ),
                    movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                    regionRepository = FakeRegionRepository(),
                )

            setContent {
                // A 360dp phone - the width at which the old row squeezed its title to one letter
                // per line beside the trailing countdown pill and bell.
                Box(modifier = Modifier.width(PHONE_WIDTH)) {
                    UpcomingReleasesTab(windowSize = WindowSize.COMPACT, viewModel = viewModel, showReminderControls = true)
                }
            }

            val titles = onAllNodesWithText(LONG_TITLE)
            titles.assertCountEquals(2) // Next-up card + timeline row
            repeat(2) { index -> assertTrue(titles[index].getBoundsInRoot().width >= MIN_TITLE_WIDTH) }
            // A date more than a week out reads "Jan 1, 2099" (year shown: not this year), not raw ISO.
            onAllNodesWithText(FAR_DATE_LABEL).assertCountEquals(2) // Next-up card + timeline row
        }

    @Test
    fun testMediumWidth_putsTheCountdownPillBesideTheTitle() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = BELL_MOVIE_ID,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = LONG_TITLE,
                                posterPath = null,
                                date = LocalDate(2099, 1, 1),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }

            setContent {
                Box(modifier = Modifier.width(MEDIUM_WIDTH)) {
                    UpcomingReleasesTab(
                        windowSize = WindowSize.MEDIUM,
                        viewModel =
                            UpcomingReleasesScreenModel(
                                fakeTrackedMediaRepository,
                                FakeTvDetailCacheRepository(),
                                movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                                regionRepository = FakeRegionRepository(),
                            ),
                    )
                }
            }

            // Index 1: the timeline row (index 0 is the Next-up card).
            val title = onAllNodesWithText(LONG_TITLE, useUnmergedTree = true)[1].getBoundsInRoot()
            val pill = onAllNodesWithText(FAR_DATE_LABEL, useUnmergedTree = true)[1].getBoundsInRoot()
            assertTrue(pill.left >= title.right)
        }

    /** A movie re-dated to the viewer's region shows that region's flag beside the date. */
    @Test
    fun testMovieDatedByRegion_showsTheRegionFlag() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = FLAG_MOVIE_ID,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = FLAG_MOVIE_TITLE,
                                posterPath = null,
                                date = LocalDate.parse(FLAG_PRIMARY_DATE),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }
            val movieDetailCacheRepository =
                FakeMovieDetailCacheRepository().apply {
                    seedCached(
                        FLAG_MOVIE_ID.toLong(),
                        MovieDetail(
                            releaseDate = FLAG_PRIMARY_DATE,
                            releaseDates =
                                ReleaseDatesResponse(
                                    results =
                                        listOf(
                                            ReleaseDatesResult(
                                                iso3166 = FLAG_REGION,
                                                releaseDates =
                                                    listOf(
                                                        ReleaseDateItem(releaseDate = FLAG_REGIONAL_DATE, type = THEATRICAL_TYPE),
                                                    ),
                                            ),
                                        ),
                                ),
                        ),
                    )
                }
            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = movieDetailCacheRepository,
                            regionRepository = FakeRegionRepository(selectedRegion = FLAG_REGION),
                        ),
                )
            }

            onAllNodesWithText(FLAG_EMOJI, substring = true).assertCountEquals(EXPECTED_FLAGGED_LABELS)
        }

    /** No cached detail yet: the row keeps TMDB's primary date, which isn't any region's - no flag. */
    @Test
    fun testMovieOnItsPrimaryDate_showsNoFlag() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedUpcoming(
                        listOf(
                            UpcomingMediaItem(
                                id = FLAG_MOVIE_ID,
                                mediaType = MediaTypeConstant.MOVIE,
                                title = FLAG_MOVIE_TITLE,
                                posterPath = null,
                                date = LocalDate.parse(FLAG_PRIMARY_DATE),
                                dateKind = UpcomingDateKind.MOVIE_RELEASE,
                            ),
                        ),
                    )
                }
            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            FakeTvDetailCacheRepository(),
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(selectedRegion = FLAG_REGION),
                        ),
                )
            }

            onAllNodesWithText(FLAG_MOVIE_TITLE, useUnmergedTree = true).assertCountEquals(EXPECTED_FLAGGED_LABELS)
            onAllNodesWithText(FLAG_EMOJI, substring = true).assertCountEquals(0)
        }

    /** An episode is labelled with its network's country (where the air date applies). */
    @Test
    fun testEpisode_showsItsNetworksCountryFlag() =
        runComposeUiTest {
            val fakeTrackedMediaRepository =
                FakeTrackedMediaRepository().apply {
                    seedTrackedTvShows(listOf(TrackedTvShowSummary(id = FLAG_SHOW_ID, title = FLAG_SHOW_TITLE, posterPath = null)))
                }
            val fakeTvDetailCacheRepository =
                FakeTvDetailCacheRepository().apply {
                    seedCached(
                        FLAG_SHOW_ID.toLong(),
                        TvDetail(networks = listOf(Network(name = FLAG_NETWORK, originCountry = FLAG_REGION))),
                    )
                    seedCachedSeason(
                        tvId = FLAG_SHOW_ID.toLong(),
                        season =
                            TvSeasonDetail(
                                seasonNumber = 1,
                                episodes = listOf(Episode(episodeNumber = 1, airDate = FLAG_PRIMARY_DATE)),
                            ),
                    )
                }
            setContent {
                UpcomingReleasesTab(
                    windowSize = WindowSize.COMPACT,
                    viewModel =
                        UpcomingReleasesScreenModel(
                            fakeTrackedMediaRepository,
                            fakeTvDetailCacheRepository,
                            movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                            regionRepository = FakeRegionRepository(),
                        ),
                )
            }

            onAllNodesWithText(FLAG_EMOJI, substring = true).assertCountEquals(EXPECTED_FLAGGED_LABELS)
        }

    private companion object {
        const val FLAG_SHOW_ID = 88
        const val FLAG_SHOW_TITLE = "Network Show"
        const val FLAG_NETWORK = "Star"
        const val FLAG_MOVIE_ID = 77
        const val FLAG_MOVIE_TITLE = "Regional Movie"
        const val FLAG_REGION = "IN"
        const val FLAG_EMOJI = "\uD83C\uDDEE\uD83C\uDDF3"
        const val FLAG_PRIMARY_DATE = "2099-01-01"
        const val FLAG_REGIONAL_DATE = "2099-02-01T00:00:00.000Z"
        const val THEATRICAL_TYPE = 3

        // The movie shows twice: the Next-up banner and its timeline row.
        const val EXPECTED_FLAGGED_LABELS = 2
        const val BELL_MOVIE_ID = 77
        const val BELL_MOVIE_TITLE = "Bell Movie"
        const val REMIND_ME_DESCRIPTION = "Remind me"
        const val REMINDER_SET_DESCRIPTION = "Reminder set - tap to remove"
        const val LONG_TITLE = "Slow Horses"
        const val FAR_DATE_LABEL = "Jan 1, 2099"
        val PHONE_WIDTH = 360.dp
        val MEDIUM_WIDTH = 700.dp
        val MIN_TITLE_WIDTH = 80.dp
    }
}
