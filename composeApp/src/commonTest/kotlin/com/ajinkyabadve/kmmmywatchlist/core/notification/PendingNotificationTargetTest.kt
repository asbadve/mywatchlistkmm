package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private object PendingNotificationTargetTestConstant {
    const val TV_SHOW_ID = 108978L
    const val SEASON_NUMBER = 4
    const val EPISODE_NUMBER = 5
    const val PERSON_ID = 500L
}

class PendingNotificationTargetTest {
    @AfterTest
    fun tearDown() {
        // Reset the shared singleton so a target set by one test can't leak into the next.
        PendingNotificationTarget.consume()
    }

    private fun episodeTarget() =
        EpisodeNotificationTarget(
            PendingNotificationTargetTestConstant.TV_SHOW_ID,
            PendingNotificationTargetTestConstant.SEASON_NUMBER,
            PendingNotificationTargetTestConstant.EPISODE_NUMBER,
        )

    @Test
    fun testSetMakesTheEpisodeTargetCurrent() {
        val target = episodeTarget()

        PendingNotificationTarget.set(target)

        assertEquals(target, PendingNotificationTarget.current)
    }

    @Test
    fun testSetMakesThePersonTargetCurrent() {
        val target = PersonNotificationTarget(PendingNotificationTargetTestConstant.PERSON_ID)

        PendingNotificationTarget.set(target)

        assertEquals(target, PendingNotificationTarget.current)
    }

    @Test
    fun testConsumeClearsCurrentWithoutReturningIt() {
        val target = episodeTarget()
        PendingNotificationTarget.set(target)

        PendingNotificationTarget.consume()

        assertNull(PendingNotificationTarget.current)
    }

    @Test
    fun testSettingTheSameTargetAgainAfterConsumeIsObservableAsANewChange() {
        val target = episodeTarget()
        PendingNotificationTarget.set(target)
        PendingNotificationTarget.consume()

        PendingNotificationTarget.set(target)

        // Equal by value to the first target, but this is the point: App.kt's LaunchedEffect keys
        // on this transition from null (post-consume) to non-null again, not on object identity or
        // value inequality - a second tap of the exact same notification must still navigate.
        assertEquals(target, PendingNotificationTarget.current)
    }
}
