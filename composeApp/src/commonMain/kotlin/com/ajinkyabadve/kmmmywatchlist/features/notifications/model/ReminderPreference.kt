package com.ajinkyabadve.kmmmywatchlist.features.notifications.model

import kotlinx.datetime.LocalTime

/** The global release-reminder preferences: whether reminders are on, and when in the day they
 *  fire (local time). */
data class ReminderPreference(
    val enabled: Boolean,
    val time: LocalTime,
)
