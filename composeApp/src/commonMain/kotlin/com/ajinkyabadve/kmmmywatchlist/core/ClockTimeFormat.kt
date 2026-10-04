package com.ajinkyabadve.kmmmywatchlist.core

import kotlinx.datetime.LocalTime

private object ClockTimeFormatConstant {
    const val NOON_HOUR = 12
    const val MINUTE_DIGITS = 2
    const val PAD_CHAR = '0'
}

/**
 * Formats [time] the way the device shows times: `09:00` / `21:30` in 24-hour mode, `9:00 AM` /
 * `9:30 PM` otherwise. [amLabel]/[pmLabel] come from string resources so they localize. Pure so
 * it can be unit-tested; kotlinx-datetime has no locale-aware time formatting of its own.
 */
fun formatClockTime(
    time: LocalTime,
    is24Hour: Boolean,
    amLabel: String,
    pmLabel: String,
): String {
    val minute = time.minute.toString().padStart(ClockTimeFormatConstant.MINUTE_DIGITS, ClockTimeFormatConstant.PAD_CHAR)
    if (is24Hour) {
        return time.hour.toString().padStart(ClockTimeFormatConstant.MINUTE_DIGITS, ClockTimeFormatConstant.PAD_CHAR) + ":" + minute
    }
    val hour12 = (time.hour % ClockTimeFormatConstant.NOON_HOUR).takeIf { it != 0 } ?: ClockTimeFormatConstant.NOON_HOUR
    val period = if (time.hour < ClockTimeFormatConstant.NOON_HOUR) amLabel else pmLabel
    return "$hour12:$minute $period"
}
