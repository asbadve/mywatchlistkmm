package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaSnapshot
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private object MediaActionsStateTestConstant {
    const val MOVIE_ID = 42L
    const val SESSION_ID = "session_abc"
    const val ACCOUNT_ID = 100L
    const val TITLE = "Upcoming Movie"
    const val RELEASE_DATE = "2099-01-01"
    val SNAPSHOT =
        TrackedMediaSnapshot(
            id = MOVIE_ID.toInt(),
            mediaType = MediaTypeConstant.MOVIE,
            title = TITLE,
            posterPath = null,
            releaseDate = RELEASE_DATE,
            voteAverage = 0.0,
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MediaActionsStateTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeAccountMediaRepository = FakeAccountMediaRepository()
    private val fakeTrackedMediaRepository = FakeTrackedMediaRepository()

    // Built in a standalone runTest{}, isolated from each test's own runTest(testDispatcher){} -
    // see MovieListScreenModelTest's identical setup for why resolving this inline inside a test
    // body confuses UnconfinedTestDispatcher's eager execution of the state holder's own launch{}.
    private lateinit var forbiddenException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            forbiddenException = HttpExceptionsTestFactory.create(HttpStatusCode.Forbidden)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun state(snapshot: TrackedMediaSnapshot? = MediaActionsStateTestConstant.SNAPSHOT) =
        MediaActionsState(
            MediaTypeConstant.MOVIE,
            MediaActionsStateTestConstant.MOVIE_ID,
            CoroutineScope(testDispatcher),
            fakeAccountMediaRepository,
            fakeTrackedMediaRepository,
            trackedMediaSnapshot = { snapshot },
        )

    private fun tvState() =
        MediaActionsState(
            MediaTypeConstant.TV,
            MediaActionsStateTestConstant.MOVIE_ID,
            CoroutineScope(testDispatcher),
            fakeAccountMediaRepository,
            fakeTrackedMediaRepository,
        )

    /** Before `load()` is ever called, the icons must show a ghost/shimmer, not a guessed state. */
    @Test
    fun testStartsLoading() {
        assertTrue(state().uiState.value.isLoading)
    }

    @Test
    fun testLoadClearsLoadingOnSuccess() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = true, watchlist = false))
            val mediaActionsState = state()

            mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID)

            assertFalse(mediaActionsState.uiState.value.isLoading)
            assertTrue(mediaActionsState.uiState.value.isFavorite)
        }

    /** A failed pre-check still clears the shimmer - it doesn't get stuck loading forever. */
    @Test
    fun testLoadClearsLoadingOnFailure() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.accountStatesResult = Result.failure(IOException("Mock network failure"))
            val mediaActionsState = state()

            mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID)

            assertFalse(mediaActionsState.uiState.value.isLoading)
        }

    /** Toggling doesn't wait on `isLoading` - an optimistic toggle updates immediately either way. */
    @Test
    fun testToggleFavoriteWorksWhileStillLoading() {
        val mediaActionsState = state()

        mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        assertTrue(mediaActionsState.uiState.value.isFavorite)
    }

    /** Removing while offline queues the delete locally instead of bouncing the icon back. */
    @Test
    fun testTurningFavoriteOffWhileOfflineQueuesTheLocalDeleteAndKeepsTheIconOff() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = true))
            fakeAccountMediaRepository.setFavoriteResult = Result.failure(IOException("Mock network failure"))
            val mediaActionsState = state()
            mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID)

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertFalse(mediaActionsState.uiState.value.isFavorite)
            assertEquals(
                listOf(Triple(MediaActionsStateTestConstant.MOVIE_ID.toInt(), MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)),
                fakeTrackedMediaRepository.markPendingDeleteCalls,
            )
            assertTrue(fakeTrackedMediaRepository.confirmDeleteCalls.isEmpty())
        }

    /** A real server-side rejection (not offline) rolls both the icon and the local queue back. */
    @Test
    fun testTurningFavoriteOffFailingOutrightRestoresTheIconAndClearsTheQueuedDelete() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = true))
            fakeAccountMediaRepository.setFavoriteResult = Result.failure(forbiddenException)
            val mediaActionsState = state()
            mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID)

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertTrue(mediaActionsState.uiState.value.isFavorite)
            assertEquals(
                listOf(Triple(MediaActionsStateTestConstant.MOVIE_ID.toInt(), MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)),
                fakeTrackedMediaRepository.clearPendingDeleteCalls,
            )
        }

    /** A successful removal hard-deletes the queued row rather than leaving it marked pending. */
    @Test
    fun testTurningFavoriteOffSuccessfullyConfirmsTheDelete() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = true))
            val mediaActionsState = state()
            mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID)

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertEquals(
                listOf(Triple(MediaActionsStateTestConstant.MOVIE_ID.toInt(), MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)),
                fakeTrackedMediaRepository.confirmDeleteCalls,
            )
        }

    /** Favoriting a TV show ON is exactly the moment the episode-alerts opt-in prompt should fire. */
    @Test
    fun testFavoritingTvShowOnRequestsEpisodeAlertPrompt() {
        val mediaActionsState = tvState()

        mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        assertTrue(mediaActionsState.shouldPromptForEpisodeAlerts.value)
    }

    /** Watchlisting a TV show ON also qualifies - either action is a valid trigger. */
    @Test
    fun testWatchlistingTvShowOnRequestsEpisodeAlertPrompt() {
        val mediaActionsState = tvState()

        mediaActionsState.toggleWatchlist(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        assertTrue(mediaActionsState.shouldPromptForEpisodeAlerts.value)
    }

    /** Movies have no episode concept - favoriting one never requests the prompt. */
    @Test
    fun testFavoritingMovieOnNeverRequestsEpisodeAlertPrompt() {
        val mediaActionsState = state()

        mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        assertFalse(mediaActionsState.shouldPromptForEpisodeAlerts.value)
    }

    /** Turning a TV show's favorite OFF is not a trigger - only turning it ON is. */
    @Test
    fun testUnfavoritingTvShowDoesNotRequestEpisodeAlertPrompt() {
        fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = true))
        val mediaActionsState = tvState()
        runTest(testDispatcher) { mediaActionsState.load(MediaActionsStateTestConstant.SESSION_ID) }

        mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        assertFalse(mediaActionsState.shouldPromptForEpisodeAlerts.value)
    }

    @Test
    fun testConsumeEpisodeAlertPromptResetsTheFlag() {
        val mediaActionsState = tvState()
        mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

        mediaActionsState.consumeEpisodeAlertPrompt()

        assertFalse(mediaActionsState.shouldPromptForEpisodeAlerts.value)
    }

    /** The Upcoming-timeline defect: favoriting a title for the first time must write it locally. */
    @Test
    fun testTurningFavoriteOnWritesTheTitleLocally() =
        runTest(testDispatcher) {
            val mediaActionsState = state()

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertEquals(
                listOf(MediaActionsStateTestConstant.SNAPSHOT to AccountMediaCategory.FAVORITES),
                fakeTrackedMediaRepository.trackLocallyCalls,
            )
        }

    @Test
    fun testTurningWatchlistOnWritesTheTitleLocally() =
        runTest(testDispatcher) {
            val mediaActionsState = state()

            mediaActionsState.toggleWatchlist(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertEquals(
                listOf(MediaActionsStateTestConstant.SNAPSHOT to AccountMediaCategory.WATCHLIST),
                fakeTrackedMediaRepository.trackLocallyCalls,
            )
        }

    /** Offline, the heart stays on - so the title should be on the timeline too. */
    @Test
    fun testTurningFavoriteOnWhileOfflineStillWritesTheTitleLocally() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.setFavoriteResult = Result.failure(IOException("Mock network failure"))
            val mediaActionsState = state()

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertTrue(mediaActionsState.uiState.value.isFavorite)
            assertEquals(
                listOf(MediaActionsStateTestConstant.SNAPSHOT to AccountMediaCategory.FAVORITES),
                fakeTrackedMediaRepository.trackLocallyCalls,
            )
        }

    /** A real rejection rolls the heart back, so nothing may be written. */
    @Test
    fun testTurningFavoriteOnFailingOutrightWritesNothing() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.setFavoriteResult = Result.failure(forbiddenException)
            val mediaActionsState = state()

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertFalse(mediaActionsState.uiState.value.isFavorite)
            assertTrue(fakeTrackedMediaRepository.trackLocallyCalls.isEmpty())
        }

    /** Before the screen's detail has loaded there's nothing to write - only revive an existing row. */
    @Test
    fun testTurningFavoriteOnWithoutASnapshotOnlyRevivesAnExistingRow() =
        runTest(testDispatcher) {
            val mediaActionsState = state(snapshot = null)

            mediaActionsState.toggleFavorite(MediaActionsStateTestConstant.ACCOUNT_ID, MediaActionsStateTestConstant.SESSION_ID)

            assertTrue(fakeTrackedMediaRepository.trackLocallyCalls.isEmpty())
            assertEquals(
                listOf(Triple(MediaActionsStateTestConstant.MOVIE_ID.toInt(), MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)),
                fakeTrackedMediaRepository.clearPendingDeleteCalls,
            )
        }
}
