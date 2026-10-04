package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReleaseReminderHeroActionUiTest {
    @Test
    fun testNotSet_showsRemindMeAndTapAddsTheReminder() =
        runComposeUiTest {
            val calls = mutableListOf<Boolean>()
            setContent {
                ReleaseReminderHeroAction(isSet = false, reminderTime = null, contentColor = Color.Black, onSetReminder = { calls.add(it) })
            }

            onNodeWithText(REMIND_ME).performClick()
            // Desktop's permission requester always grants, so the tap goes straight through.
            waitUntil { calls.isNotEmpty() }

            assertEquals(listOf(true), calls)
        }

    @Test
    fun testSet_showsTheReminderTimeCaptionAndTapRemovesIt() =
        runComposeUiTest {
            val calls = mutableListOf<Boolean>()
            setContent {
                ReleaseReminderHeroAction(
                    isSet = true,
                    reminderTime = NINE_AM,
                    contentColor = Color.Black,
                    onSetReminder = { calls.add(it) },
                )
            }

            onNodeWithText(TIME_CAPTION).assertExists()
            onNodeWithText(REMINDER_SET).performClick()

            assertEquals(listOf(false), calls)
        }

    private companion object {
        const val REMIND_ME = "Remind me"
        const val REMINDER_SET = "Reminder set"

        // Desktop's is24HourClock() is always false, so the 12-hour form is expected.
        const val TIME_CAPTION = "We'll remind you at 9:00 AM - change it in Settings"
        val NINE_AM = LocalTime(9, 0)
    }
}
