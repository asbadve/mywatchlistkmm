package com.ajinkyabadve.kmmmywatchlist.core.ui.splash

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object SplashScreenUiTestConstant {
    const val SHORT_DURATION_MILLIS = 10L
    const val WAIT_TIMEOUT_MILLIS = 5_000L
}

@OptIn(ExperimentalTestApi::class)
class SplashScreenUiTest {
    @Test
    fun testSplashScreen_rendersTheWordmark() =
        runComposeUiTest {
            setContent {
                SplashScreen(onFinished = {})
            }

            onAllNodesWithText("MyWatch")[0].assertExists()
            onAllNodesWithText("List")[0].assertExists()
        }

    @Test
    fun testSplashScreen_callsOnFinishedAfterItsDuration() =
        runComposeUiTest {
            var finished = false
            setContent {
                SplashScreen(onFinished = { finished = true }, durationMillis = SplashScreenUiTestConstant.SHORT_DURATION_MILLIS)
            }

            waitUntil(timeoutMillis = SplashScreenUiTestConstant.WAIT_TIMEOUT_MILLIS) { finished }

            assertEquals(true, finished)
        }
}
