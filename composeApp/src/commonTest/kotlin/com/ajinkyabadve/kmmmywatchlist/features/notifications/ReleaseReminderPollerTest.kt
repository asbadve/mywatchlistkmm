package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.EPISODE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.EPISODE_NUMBER
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.MOVIE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.NOW
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REGION_US
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.RELEASE_IN_FIVE_DAYS
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.RELEASE_SLIPPED
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REMINDER_TIME
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.SEASON_NUMBER
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.TODAY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.episodeReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.movieReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderConstant
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseReminderPollerTest {
    private val repository = FakeReleaseReminderRepository(ReminderPreference(enabled = true, time = REMINDER_TIME))
    private val scheduler = FakeReminderScheduler()
    private val movieRepository = FakeMovieRepository()
    private val tvRepository = FakeTvRepository()
    private val poller =
        ReleaseReminderPoller(
            repository = repository,
            movieRepository = movieRepository,
            tvRepository = tvRepository,
            regionRepository = FakeRegionRepository(),
            coordinator = ReleaseReminderCoordinator(repository, scheduler, timeZone = { TimeZone.UTC }, now = { NOW }),
            today = { TODAY },
        )

    @Test
    fun testSlippedMovieDate_updatesTheRowAndReschedules() =
        runTest {
            repository.seed(movieReminder(RELEASE_IN_FIVE_DAYS))
            movieRepository.getMovieDetailsResult = Result.success(MovieDetail(releaseDate = RELEASE_SLIPPED.toString()))

            poller.poll()

            assertEquals(listOf(MOVIE_KEY to RELEASE_SLIPPED), repository.updateReleaseDateCalls)
            assertEquals(RELEASE_SLIPPED.atTime(REMINDER_TIME), scheduler.scheduled[MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY)])
        }

    @Test
    fun testSlippedEpisodeDate_isReadFromItsSeason() =
        runTest {
            repository.seed(episodeReminder(RELEASE_IN_FIVE_DAYS))
            tvRepository.getSeasonDetailsResultsByNumber[SEASON_NUMBER] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = SEASON_NUMBER,
                        episodes = listOf(Episode(episodeNumber = EPISODE_NUMBER, airDate = RELEASE_SLIPPED.toString())),
                    ),
                )

            poller.poll()

            assertEquals(listOf(EPISODE_KEY to RELEASE_SLIPPED), repository.updateReleaseDateCalls)
        }

    @Test
    fun testUnchangedDate_doesNotRewriteTheRow() =
        runTest {
            repository.seed(movieReminder(RELEASE_IN_FIVE_DAYS))
            movieRepository.getMovieDetailsResult = Result.success(MovieDetail(releaseDate = RELEASE_IN_FIVE_DAYS.toString()))

            poller.poll()

            assertTrue(repository.updateReleaseDateCalls.isEmpty())
        }

    @Test
    fun testOneFailure_doesNotStopTheOthers() =
        runTest {
            repository.seed(movieReminder(RELEASE_IN_FIVE_DAYS), episodeReminder(RELEASE_IN_FIVE_DAYS))
            movieRepository.getMovieDetailsResult = Result.failure(IOException(NETWORK_FAILURE))
            tvRepository.getSeasonDetailsResultsByNumber[SEASON_NUMBER] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = SEASON_NUMBER,
                        episodes = listOf(Episode(episodeNumber = EPISODE_NUMBER, airDate = RELEASE_SLIPPED.toString())),
                    ),
                )

            poller.poll()

            assertEquals(listOf(EPISODE_KEY to RELEASE_SLIPPED), repository.updateReleaseDateCalls)
            // The failed movie still keeps its previously scheduled reminder.
            assertTrue(MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY) in scheduler.scheduled)
        }

    @Test
    fun testRegionChange_movesTheReminderToTheNewRegionsDate() =
        runTest {
            // Saved while the viewer was in the US; they've since switched to Japan, where the
            // film opens weeks later (seen live for "Other Mommy": US Oct 9, JP Nov 20).
            repository.seed(movieReminder(RELEASE_IN_FIVE_DAYS))
            movieRepository.getMovieDetailsResult =
                Result.success(
                    MovieDetail(
                        releaseDate = RELEASE_IN_FIVE_DAYS.toString(),
                        releaseDates =
                            ReleaseDatesResponse(
                                results =
                                    listOf(
                                        ReleaseDatesResult(REGION_US, listOf(theatrical(RELEASE_IN_FIVE_DAYS))),
                                        ReleaseDatesResult(REGION_JP, listOf(theatrical(RELEASE_SLIPPED))),
                                    ),
                            ),
                    ),
                )
            val japanPoller =
                ReleaseReminderPoller(
                    repository = repository,
                    movieRepository = movieRepository,
                    tvRepository = tvRepository,
                    regionRepository = FakeRegionRepository(selectedRegion = REGION_JP, fallbackRegion = REGION_US),
                    coordinator = ReleaseReminderCoordinator(repository, scheduler, timeZone = { TimeZone.UTC }, now = { NOW }),
                    today = { TODAY },
                )

            japanPoller.poll()

            val stored = repository.allReminders().single()
            assertEquals(RELEASE_SLIPPED, stored.releaseDate)
            assertEquals(ReleaseSource(ReleaseSourceKind.THEATRICAL, REGION_JP).encode(), stored.releaseSource)
            assertEquals(RELEASE_SLIPPED.atTime(REMINDER_TIME), scheduler.scheduled[MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY)])
        }

    private fun theatrical(date: LocalDate) =
        ReleaseDateItem(releaseDate = date.toString() + ISO_TIME_SUFFIX, type = ReleaseType.THEATRICAL.tmdbValue)

    @Test
    fun testExpiredRows_areRemoved() =
        runTest {
            val longPast = TODAY.minus(DatePeriod(days = ReleaseReminderConstant.EXPIRY_DAYS + 1))
            repository.seed(movieReminder(longPast))

            poller.poll()

            assertTrue(repository.allReminders().isEmpty())
        }

    private companion object {
        const val NETWORK_FAILURE = "offline"
        const val REGION_JP = "JP"
        const val ISO_TIME_SUFFIX = "T00:00:00.000Z"
    }
}
