package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCombinedCredits
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
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
class PersonDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val longBiography =
        "A remarkably long biography sentence describing a whole career. ".repeat(20)

    private fun successfulPersonDetail() =
        PersonDetail(
            id = 1,
            name = "Fake Person",
            biography = longBiography,
            combinedCredits =
                PersonCombinedCredits(
                    cast =
                        listOf(
                            PersonCredit(id = 401, mediaType = "movie", title = "Known Movie", voteCount = 100),
                        ),
                    crew =
                        listOf(
                            PersonCredit(
                                id = 402,
                                mediaType = "tv",
                                name = "Directed Show",
                                job = "Director",
                                department = "Directing",
                                voteCount = 50,
                            ),
                        ),
                ),
        )

    @Test
    fun testPersonDetailScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onTvShowClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getPersonDetailsResult = Result.success(successfulPersonDetail())
            onNodeWithText("Retry").performClick()

            onAllNodesWithText("Fake Person")[0].assertExists()
        }

    @Test
    fun testPersonDetailScreen_biographyReadMore_expandsAndCollapses() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.success(successfulPersonDetail())
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onTvShowClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Read more").assertExists()
            onNodeWithText("Read more").performClick()
            onNodeWithText("Read less").assertExists()
            onNodeWithText("Read less").performClick()
            onNodeWithText("Read more").assertExists()
        }

    @Test
    fun testPersonDetailScreen_knownForCreditClick_invokesOnMovieClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.success(successfulPersonDetail())
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)
            var clickedMovieId: Long? = null

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = { clickedMovieId = it },
                    onTvShowClicked = {},
                    viewModel = viewModel,
                )
            }

            onAllNodesWithText("Known Movie")[0].performClick()
            assertEquals(401L, clickedMovieId)
        }

    @Test
    fun testPersonDetailScreen_filmographyRowClick_invokesOnTvShowClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.success(successfulPersonDetail())
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)
            var clickedTvShowId: Long? = null

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onTvShowClicked = { clickedTvShowId = it },
                    viewModel = viewModel,
                )
            }

            onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(FILMOGRAPHY_ITEM_INDEX)
            onNodeWithText("Directed Show").performClick()
            assertEquals(402L, clickedTvShowId)
        }

    @Test
    fun testPersonDetailScreen_filmographyMediaFilter_moviesOnlyHidesTvCredits() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.success(successfulPersonDetail())
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onTvShowClicked = {},
                    viewModel = viewModel,
                )
            }

            onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(FILMOGRAPHY_ITEM_INDEX)
            onNodeWithText("Directed Show").assertExists()
            onNodeWithText("Movies").performClick()
            onNodeWithText("Directed Show").assertDoesNotExist()
        }

    @Test
    fun testPersonDetailScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPersonDetailsResult = Result.success(successfulPersonDetail())
                }
            val viewModel = PersonDetailScreenModel(personId = 1, personRepository = fakeRepository)
            var backClicked = false

            setContent {
                PersonDetailScreen(
                    personId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = { backClicked = true },
                    onMovieClicked = {},
                    onTvShowClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }

    private companion object {
        // CompactPersonDetailContent's LazyColumn item order: header, links, biography, known
        // for, photos, filmography - the last of which isn't composed within the default test
        // viewport, so it must be scrolled into view before its contents can be interacted with.
        const val FILMOGRAPHY_ITEM_INDEX = 5
    }
}
