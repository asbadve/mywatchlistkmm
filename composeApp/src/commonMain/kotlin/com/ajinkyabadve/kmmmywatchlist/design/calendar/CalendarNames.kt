package com.ajinkyabadve.kmmmywatchlist.design.calendar

import kotlinx.datetime.DayOfWeek
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.month_april
import mywatchlist.composeapp.generated.resources.month_august
import mywatchlist.composeapp.generated.resources.month_december
import mywatchlist.composeapp.generated.resources.month_february
import mywatchlist.composeapp.generated.resources.month_january
import mywatchlist.composeapp.generated.resources.month_july
import mywatchlist.composeapp.generated.resources.month_june
import mywatchlist.composeapp.generated.resources.month_march
import mywatchlist.composeapp.generated.resources.month_may
import mywatchlist.composeapp.generated.resources.month_november
import mywatchlist.composeapp.generated.resources.month_october
import mywatchlist.composeapp.generated.resources.month_september
import mywatchlist.composeapp.generated.resources.weekday_friday
import mywatchlist.composeapp.generated.resources.weekday_monday
import mywatchlist.composeapp.generated.resources.weekday_saturday
import mywatchlist.composeapp.generated.resources.weekday_sunday
import mywatchlist.composeapp.generated.resources.weekday_thursday
import mywatchlist.composeapp.generated.resources.weekday_tuesday
import mywatchlist.composeapp.generated.resources.weekday_wednesday
import org.jetbrains.compose.resources.StringResource

/*
 * Localized month and short weekday names. kotlinx-datetime's Month/DayOfWeek names are English
 * enum constants and the library has no locale-aware formatting, so these map to string resources.
 */

/** The full month name for [monthNumber] (1-12). */
fun monthNameRes(monthNumber: Int): StringResource =
    when (monthNumber) {
        1 -> Res.string.month_january
        2 -> Res.string.month_february
        3 -> Res.string.month_march
        4 -> Res.string.month_april
        5 -> Res.string.month_may
        6 -> Res.string.month_june
        7 -> Res.string.month_july
        8 -> Res.string.month_august
        9 -> Res.string.month_september
        10 -> Res.string.month_october
        11 -> Res.string.month_november
        else -> Res.string.month_december
    }

/** The short (three-letter) weekday name for [dayOfWeek]. */
fun weekdayNameRes(dayOfWeek: DayOfWeek): StringResource =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> Res.string.weekday_monday
        DayOfWeek.TUESDAY -> Res.string.weekday_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.weekday_wednesday
        DayOfWeek.THURSDAY -> Res.string.weekday_thursday
        DayOfWeek.FRIDAY -> Res.string.weekday_friday
        DayOfWeek.SATURDAY -> Res.string.weekday_saturday
        else -> Res.string.weekday_sunday
    }
