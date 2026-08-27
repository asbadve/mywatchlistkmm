package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private object PendingEpisodeNotificationTargetTestConstant {
    const val TV_SHOW_ID = 108978L
    const val SEASON_NUMBER = 4
    const val EPISODE_NUMBER = 5
}

class PendingEpisodeNotificationTargetTest {
    @AfterTest
    fun tearDown() {
        // Reset the shared singleton so a target set by one test can't leak into the next.
        PendingEpisodeNotificationTarget.consume()
    }

    @Test
    fun testSetMakesTheTargetCurrent() {
        val target =
            EpisodeNotificationTarget(
                PendingEpisodeNotificationTargetTestConstant.TV_SHOW_ID,
                PendingEpisodeNotificationTargetTestConstant.SEASON_NUMBER,
                PendingEpisodeNotificationTargetTestConstant.EPISODE_NUMBER,
            )

        PendingEpisodeNotificationTarget.set(target)

        assertEquals(target, PendingEpisodeNotificationTarget.current)
    }

    @Test
    fun testConsumeClearsCurrentWithoutReturningIt() {
        val target =
            EpisodeNotificationTarget(
                PendingEpisodeNotificationTargetTestConstant.TV_SHOW_ID,
                PendingEpisodeNotificationTargetTestConstant.SEASON_NUMBER,
                PendingEpisodeNotificationTargetTestConstant.EPISODE_NUMBER,
            )
        PendingEpisodeNotificationTarget.set(target)

        PendingEpisodeNotificationTarget.consume()

        assertNull(PendingEpisodeNotificationTarget.current)
    }

    @Test
    fun testSettingTheSameTargetAgainAfterConsumeIsObservableAsANewChange() {
        val target =
            EpisodeNotificationTarget(
                PendingEpisodeNotificationTargetTestConstant.TV_SHOW_ID,
                PendingEpisodeNotificationTargetTestConstant.SEASON_NUMBER,
                PendingEpisodeNotificationTargetTestConstant.EPISODE_NUMBER,
            )
        PendingEpisodeNotificationTarget.set(target)
        PendingEpisodeNotificationTarget.consume()

        PendingEpisodeNotificationTarget.set(target)

        // Equal by value to the first target, but this is the point: App.kt's LaunchedEffect keys
        // on this transition from null (post-consume) to non-null again, not on object identity or
        // value inequality - a second tap of the exact same notification must still navigate.
        assertEquals(target, PendingEpisodeNotificationTarget.current)
    }
}
