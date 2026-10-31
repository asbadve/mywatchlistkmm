package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * Activity recreation (theme/locale change) and process death, emulated: [StateRestorationTester]
 * disposes the composition and rebuilds it from saved state, which is what the platform does.
 *
 * Lives in androidInstrumentedTest, not commonTest: Compose Multiplatform's skiko targets (desktop,
 * iOS, web) don't implement [StateRestorationTester] yet - `emulateSaveAndRestore()` throws
 * `NotImplementedError` (https://youtrack.jetbrains.com/issue/CMP-7992). Android is also the
 * platform that actually recreates the screen. The save/restore logic itself is covered on every
 * target by `TopLevelBackStackTest`. Move this back to commonTest once CMP-7992 is fixed.
 */
@OptIn(ExperimentalTestApi::class)
class TopLevelBackStackUiTest {
    @Test
    fun testOpenDetailScreenIsStillShownAfterRecreation() =
        runComposeUiTest {
            val restorationTester = StateRestorationTester(this)
            restorationTester.setContent { BackStackHarness() }

            onNodeWithText(OPEN_MOVIE_LABEL).performClick()
            onNodeWithTag(CURRENT_KEY_TAG).assertTextEquals(MovieDetailKey(MOVIE_ID).toString())

            restorationTester.emulateSaveAndRestore()

            onNodeWithTag(CURRENT_KEY_TAG).assertTextEquals(MovieDetailKey(MOVIE_ID).toString())
            // And the restored stack still knows what is underneath it.
            onNodeWithText(BACK_LABEL).performClick()
            onNodeWithTag(CURRENT_KEY_TAG).assertTextEquals(TrendingKey.toString())
        }

    @Test
    fun testSelectedTabIsStillSelectedAfterRecreation() =
        runComposeUiTest {
            val restorationTester = StateRestorationTester(this)
            restorationTester.setContent { BackStackHarness() }

            onNodeWithText(MOVIES_TAB_LABEL).performClick()
            onNodeWithText(OPEN_ACCOUNT_LABEL).performClick()

            restorationTester.emulateSaveAndRestore()

            onNodeWithTag(CURRENT_KEY_TAG).assertTextEquals(AccountKey.toString())
            onNodeWithTag(TOP_LEVEL_KEY_TAG).assertTextEquals(MoviesKey.toString())
        }

    /** The real [rememberTopLevelBackStack], driven by buttons instead of the full app shell. */
    @Composable
    private fun BackStackHarness() {
        val topLevelBackStack = rememberTopLevelBackStack(TrendingKey)
        Column {
            Text(
                text = topLevelBackStack.backStack.last().toString(),
                modifier = Modifier.testTag(CURRENT_KEY_TAG),
            )
            Text(
                text = topLevelBackStack.topLevelKey.toString(),
                modifier = Modifier.testTag(TOP_LEVEL_KEY_TAG),
            )
            Button(onClick = { topLevelBackStack.add(MovieDetailKey(MOVIE_ID)) }) { Text(OPEN_MOVIE_LABEL) }
            Button(onClick = { topLevelBackStack.add(AccountKey) }) { Text(OPEN_ACCOUNT_LABEL) }
            Button(onClick = { topLevelBackStack.switchTopLevel(MoviesKey) }) { Text(MOVIES_TAB_LABEL) }
            Button(onClick = { topLevelBackStack.removeLast() }) { Text(BACK_LABEL) }
        }
    }

    private companion object {
        const val MOVIE_ID = 603L
        const val CURRENT_KEY_TAG = "current_key"
        const val TOP_LEVEL_KEY_TAG = "top_level_key"
        const val OPEN_MOVIE_LABEL = "Open movie"
        const val OPEN_ACCOUNT_LABEL = "Open account"
        const val MOVIES_TAB_LABEL = "Movies tab"
        const val BACK_LABEL = "Back"
    }
}
