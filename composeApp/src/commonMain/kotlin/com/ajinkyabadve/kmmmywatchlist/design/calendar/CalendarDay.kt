package com.ajinkyabadve.kmmmywatchlist.design.calendar

/** One cell of a [MonthCalendarCard] grid - a `null` [dayOfMonth] is a leading blank before the
 *  1st, so the days line up under the right weekday column. */
internal data class CalendarDay(
    val dayOfMonth: Int?,
    val isToday: Boolean,
    val isMarked: Boolean,
)
