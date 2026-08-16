package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private object MediaActionsStateTestConstant {
    const val MOVIE_ID = 42L
    const val SESSION_ID = "session_abc"
    const val ACCOUNT_ID = 100L
}

@OptIn(ExperimentalCoroutinesApi::class)
class MediaActionsStateTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeAccountMediaRepository = FakeAccountMediaRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun state() =
        MediaActionsState(
            MediaTypeConstant.MOVIE,
            MediaActionsStateTestConstant.MOVIE_ID,
            CoroutineScope(testDispatcher),
            fakeAccountMediaRepository,
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
}
