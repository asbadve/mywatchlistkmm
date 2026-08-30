package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeFavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class CollectionDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulCollectionDetail() =
        CollectionDetail(
            id = 1,
            name = "Fake Collection",
            parts = listOf(Movie(id = 201, title = "Part One", releaseDate = "2020-01-01")),
        )

    private fun buildRepository(): FakeMovieRepository =
        FakeMovieRepository().apply {
            getCollectionDetailsResult = Result.success(successfulCollectionDetail())
            getMovieCreditsResults[201] =
                Result.success(
                    Credits(cast = listOf(CastMember(id = 301, name = "Actor One", character = "Hero", order = 0))),
                )
        }

    @Test
    fun testCollectionDetailScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getCollectionDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = fakeRepository,
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getCollectionDetailsResult = Result.success(successfulCollectionDetail())
            onNodeWithText("Retry").performClick()

            onAllNodesWithText("Fake Collection")[0].assertExists()
        }

    @Test
    fun testCollectionDetailScreen_moviePartClick_invokesOnMovieClicked() =
        runComposeUiTest {
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = buildRepository(),
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )
            var clickedMovieId: Long? = null

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = { clickedMovieId = it },
                    viewModel = viewModel,
                )
            }

            // The header grew a FollowCollectionButton, so "Part One" (the next LazyColumn item)
            // is no longer in the initially-composed viewport - same scroll-into-view need as the
            // featuredCastClick test below.
            onNode(hasScrollToIndexAction()).performScrollToIndex(1)
            onNodeWithText("Part One").performClick()
            assertEquals(201L, clickedMovieId)
        }

    @Test
    fun testCollectionDetailScreen_featuredCastClick_invokesOnPersonClicked() =
        runComposeUiTest {
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = buildRepository(),
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )
            var personId: Long? = null

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onPersonClicked = { personId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(2)
            onNodeWithText("Actor One").performClick()
            assertEquals(301L, personId)
        }

    @Test
    fun testCollectionDetailScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = buildRepository(),
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )
            var backClicked = false

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = { backClicked = true },
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }

    /** Following a collection while notifications are off shows the same opt-in prompt shape
     *  TV/person favorites already get (see NotificationOptInDialog). */
    @Test
    fun testCollectionDetailScreen_followClick_showsNotificationOptInPrompt() =
        runComposeUiTest {
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = buildRepository(),
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                )
            }

            onNodeWithText("Follow").performClick()

            onNodeWithText("Get notified when Fake Collection grows?").assertExists()
        }

    /** "Not now" marks the prompt seen (so it never shows again) without touching the
     *  notification setting. */
    @Test
    fun testCollectionDetailScreen_dismissingNotificationOptInPrompt_marksItSeen() =
        runComposeUiTest {
            val viewModel =
                CollectionDetailScreenModel(
                    collectionId = 1,
                    movieRepository = buildRepository(),
                    favoriteCollectionRepository = FakeFavoriteCollectionRepository(),
                )
            val fakeNotificationSettingsRepository = FakeNotificationSettingsRepository()

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                    notificationSettingsRepository = fakeNotificationSettingsRepository,
                )
            }
            onNodeWithText("Follow").performClick()

            onNodeWithText("Not now").performClick()

            assertEquals(true, fakeNotificationSettingsRepository.hasSeenEpisodeAlertOptInPrompt())
            onNodeWithText("Get notified when Fake Collection grows?").assertDoesNotExist()
        }

    @Test
    fun testCollectionDetailScreen_followClick_togglesFollowingState() =
        runComposeUiTest {
            val favoriteCollectionRepository = FakeFavoriteCollectionRepository()
            val viewModel = CollectionDetailScreenModel(1, buildRepository(), favoriteCollectionRepository)

            setContent {
                CollectionDetailScreen(
                    collectionId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                )
            }

            onNodeWithText("Follow").performClick()

            onNodeWithText("Following").assertExists()
        }
}
