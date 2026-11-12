package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.viewmodel.sharedAuthScreenModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ScreenModels live exactly as long as their back-stack entry (rememberAppEntryDecorators):
 * a reopened screen gets a fresh one, going back clears it, and switching tabs keeps the other
 * tabs' ones - except the deliberately shared AuthScreenModel, which every screen still gets the
 * same instance of. Driven through the real [TopLevelBackStack], as App.kt does.
 */
@OptIn(ExperimentalTestApi::class)
class AppEntryDecoratorsUiTest {
    /** Stands in for a screen's ScreenModel; records each instance and whether it was cleared. */
    private class RecordingScreenModel : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private val created = mutableMapOf<AppKey, MutableList<RecordingScreenModel>>()
    private val authScreenModels = mutableMapOf<AppKey, AuthScreenModel>()
    private val authRepository = FakeAuthRepository()

    /** What App.kt provides: one app-session store, both as the ambient owner and the shared one. */
    private val appOwner =
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }

    @Test
    fun testReopeningADetailScreenGetsAFreshScreenModelAndTheOldOneIsCleared() =
        runComposeUiTest {
            val backStack = TopLevelBackStack(TrendingKey)
            setContent { display(backStack) }

            backStack.add(DETAIL)
            waitUntilExactlyOneExists(hasText(DETAIL_TEXT))
            backStack.removeLast()
            waitUntilExactlyOneExists(hasText(TRENDING_TEXT))
            backStack.add(DETAIL)
            waitUntilExactlyOneExists(hasText(DETAIL_TEXT))

            val (first, second) = created.getValue(DETAIL)
            assertNotSame(first, second)
            assertTrue(first.cleared)
            assertFalse(second.cleared)
        }

    @Test
    fun testSwitchingTabsKeepsTheOtherTabsScreenModel() =
        runComposeUiTest {
            val backStack = TopLevelBackStack(TrendingKey)
            setContent { display(backStack) }
            waitUntilExactlyOneExists(hasText(TRENDING_TEXT))

            backStack.switchTopLevel(MoviesKey)
            waitUntilExactlyOneExists(hasText(MOVIES_TEXT))
            backStack.switchTopLevel(TrendingKey)
            waitUntilExactlyOneExists(hasText(TRENDING_TEXT))

            val trending = created.getValue(TrendingKey)
            assertSame(trending.first(), trending.last())
            assertFalse(trending.single().cleared)
        }

    @Test
    fun testEveryScreenGetsTheSameSharedAuthScreenModel() =
        runComposeUiTest {
            val backStack = TopLevelBackStack(MyFavKey)
            setContent { display(backStack) }
            waitUntilExactlyOneExists(hasText(MY_FAV_TEXT))

            backStack.add(DETAIL)
            waitUntilExactlyOneExists(hasText(DETAIL_TEXT))

            assertSame(authScreenModels.getValue(MyFavKey), authScreenModels.getValue(DETAIL))
        }

    @Composable
    private fun display(backStack: TopLevelBackStack) =
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides appOwner,
            LocalAppViewModelStoreOwner provides appOwner,
        ) {
            NavDisplay(
                backStack = backStack.backStack,
                onBack = { backStack.removeLast() },
                entryDecorators = rememberAppEntryDecorators(),
                entryProvider =
                    entryProvider {
                        entry<TrendingKey> { screen(TrendingKey, TRENDING_TEXT) }
                        entry<MoviesKey> { screen(MoviesKey, MOVIES_TEXT) }
                        entry<MyFavKey> { screen(MyFavKey, MY_FAV_TEXT) }
                        entry<MovieDetailKey> { screen(it, DETAIL_TEXT) }
                    },
            )
        }

    @Composable
    private fun screen(
        key: AppKey,
        text: String,
    ) {
        val screenModel = viewModel { RecordingScreenModel() }
        val instances = created.getOrPut(key) { mutableListOf() }
        if (instances.none { it === screenModel }) instances += screenModel
        val authScreenModel = sharedAuthScreenModel(authRepository)
        authScreenModels.getOrPut(key) { authScreenModel }
        Text(text)
    }

    private companion object {
        val DETAIL = MovieDetailKey(42)
        const val TRENDING_TEXT = "Trending screen"
        const val MOVIES_TEXT = "Movies screen"
        const val MY_FAV_TEXT = "My Fav screen"
        const val DETAIL_TEXT = "Detail screen"
    }
}
