package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedMediaSnapshot
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.UpcomingReleasesTab
import com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel.UpcomingReleasesScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * IMMEDIATE_DEFECTS.md (2026-10-10): a movie favorited from its detail screen didn't show up in
 * the Upcoming timeline until some later full sync. Renders the hero's favorite button and the
 * Upcoming tab side by side over one real [TrackedMediaRepositoryImpl] on an in-memory database -
 * the same table the timeline reads in production - with no refresh in between.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class FavoriteToUpcomingUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFavoritingAnUnreleasedMovieShowsItInUpcomingRightAway() =
        runComposeUiTest {
            val database = runBlocking { createTestDatabase() }
            val trackedMediaRepository =
                TrackedMediaRepositoryImpl(accountMediaRepository = FakeAccountMediaRepository(), databaseProvider = { database })
            val authRepository =
                FakeAuthRepository().apply {
                    saveSession(UserSession(sessionId = SESSION_ID, accountId = ACCOUNT_ID, username = USERNAME, name = USERNAME))
                }
            val mediaActionsState =
                MediaActionsState(
                    MediaTypeConstant.MOVIE,
                    MOVIE_ID,
                    CoroutineScope(testDispatcher),
                    FakeAccountMediaRepository(),
                    trackedMediaRepository,
                    trackedMediaSnapshot = { SNAPSHOT },
                ).apply { load(SESSION_ID) }
            val reminders = FakeReleaseReminderRepository()
            val upcomingScreenModel =
                UpcomingReleasesScreenModel(
                    trackedMediaRepository = trackedMediaRepository,
                    tvDetailCacheRepository = FakeTvDetailCacheRepository(),
                    releaseReminderRepository = reminders,
                    releaseReminderCoordinator = ReleaseReminderCoordinator(reminders, FakeReminderScheduler()),
                    notificationJobSync =
                        NotificationJobSync(
                            notificationSettingsRepository = FakeNotificationSettingsRepository(),
                            releaseReminderRepository = reminders,
                            scheduleJob = {},
                            cancelJob = {},
                        ),
                    movieDetailCacheRepository = FakeMovieDetailCacheRepository(),
                    regionRepository = FakeRegionRepository(),
                )
            setContent {
                Column {
                    MediaActionButtonsSection(
                        mediaId = MOVIE_ID,
                        colors = HeroColors.forTheme(isDark = true, colorScheme = lightColorScheme(), hasSystemStatusBar = false),
                        showAddToList = false,
                        authRepository = authRepository,
                        mediaActionsState = mediaActionsState,
                        listsRepository = FakeListsRepository(),
                        notificationSettingsRepository = FakeNotificationSettingsRepository(),
                    )
                    UpcomingReleasesTab(windowSize = WindowSize.COMPACT, viewModel = upcomingScreenModel)
                }
            }
            onNodeWithText(NOTHING_UPCOMING).assertExists()

            onNodeWithContentDescription(FAVORITE_CONTENT_DESCRIPTION).performClick()

            waitUntil(timeoutMillis = TIMEOUT_MILLIS) { onAllNodesWithTextCount(MOVIE_TITLE) > 0 }
            // Once as the "Next up" banner (the soonest title) and once as its timeline row.
            onAllNodesWithText(MOVIE_TITLE).assertCountEquals(EXPECTED_TITLE_NODES)
        }

    private fun ComposeUiTest.onAllNodesWithTextCount(text: String): Int = onAllNodes(hasText(text)).fetchSemanticsNodes().size

    private companion object {
        const val MOVIE_ID = 4242L
        const val MOVIE_TITLE = "Not Out Yet"
        const val SESSION_ID = "session_abc"
        const val ACCOUNT_ID = 100L
        const val USERNAME = "tester"
        const val FAVORITE_CONTENT_DESCRIPTION = "Favorite"
        const val NOTHING_UPCOMING = "Nothing upcoming"
        const val TIMEOUT_MILLIS = 5_000L
        const val EXPECTED_TITLE_NODES = 2

        // Comfortably in the future whenever this runs, so observeUpcoming's real "today" never flakes.
        const val RELEASE_DATE = "2099-01-01"
        val SNAPSHOT =
            TrackedMediaSnapshot(
                id = MOVIE_ID.toInt(),
                mediaType = MediaTypeConstant.MOVIE,
                title = MOVIE_TITLE,
                posterPath = null,
                releaseDate = RELEASE_DATE,
                voteAverage = 0.0,
            )
    }
}
