package com.ajinkyabadve.kmmmywatchlist.design.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

private object MonthCalendarConstant {
    const val DAYS_PER_WEEK = 7
    val CARD_CORNER_RADIUS = 20.dp
    val CARD_PADDING = 16.dp
    val SECTION_SPACING = 12.dp
    val CELL_SIZE = 36.dp
    val CELL_CORNER_RADIUS = 8.dp
    val TODAY_BORDER_WIDTH = 1.dp
    val MARK_DOT_SIZE = 5.dp
    val LEGEND_DOT_SIZE = 8.dp
    val LEGEND_ITEM_SPACING = 16.dp
    val LEGEND_DOT_TEXT_SPACING = 6.dp
}

/** One cell of a [MonthCalendarCard] grid - a `null` [dayOfMonth] is a leading blank before the
 *  1st, so the days line up under the right weekday column. */
internal data class CalendarDay(
    val dayOfMonth: Int?,
    val isToday: Boolean,
    val isMarked: Boolean,
)

/** The Monday-first day grid for one month. Pure, so the geometry (weekday alignment, today,
 *  marks, month length) can be asserted without a Compose harness. */
internal fun buildCalendarDays(
    year: Int,
    monthNumber: Int,
    markedDays: Set<Int>,
    today: LocalDate,
): List<CalendarDay> {
    val firstOfMonth = LocalDate(year, monthNumber, 1)
    val leadingBlanks = firstOfMonth.dayOfWeek.ordinal
    val daysInMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    val blanks = List(leadingBlanks) { CalendarDay(dayOfMonth = null, isToday = false, isMarked = false) }
    val days =
        (1..daysInMonth).map { day ->
            CalendarDay(
                dayOfMonth = day,
                isToday = today.year == year && today.monthNumber == monthNumber && today.dayOfMonth == day,
                isMarked = day in markedDays,
            )
        }
    return blanks + days
}

/**
 * A read-only month view: a card with the month title, a Monday-first day grid, a dot under each
 * day in [markedDays], a border around [today], and a two-item legend ([markedLabel],
 * [todayLabel]).
 *
 * Material3's DatePicker was the platform candidate, but it's an interactive selection control -
 * it has no per-day marker slot and its chrome (mode toggle, selection headline) doesn't fit a
 * glanceable, non-interactive "what happens this month" card.
 */
@Composable
fun MonthCalendarCard(
    year: Int,
    monthNumber: Int,
    markedDays: Set<Int>,
    today: LocalDate,
    markedLabel: String,
    todayLabel: String,
    modifier: Modifier = Modifier,
) {
    val days = remember(year, monthNumber, markedDays, today) { buildCalendarDays(year, monthNumber, markedDays, today) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MonthCalendarConstant.CARD_CORNER_RADIUS))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(MonthCalendarConstant.CARD_PADDING),
    ) {
        Text(
            text = "${stringResource(monthNameRes(monthNumber))} $year",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(MonthCalendarConstant.SECTION_SPACING))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { dayOfWeek ->
                Text(
                    text = stringResource(weekdayNameRes(dayOfWeek)).take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(MonthCalendarConstant.CELL_SIZE),
                )
            }
        }
        days.chunked(MonthCalendarConstant.DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day -> CalendarDayCell(day) }
                repeat(MonthCalendarConstant.DAYS_PER_WEEK - week.size) {
                    Spacer(modifier = Modifier.width(MonthCalendarConstant.CELL_SIZE))
                }
            }
        }
        Spacer(modifier = Modifier.height(MonthCalendarConstant.SECTION_SPACING))
        Row(horizontalArrangement = Arrangement.spacedBy(MonthCalendarConstant.LEGEND_ITEM_SPACING)) {
            CalendarLegendItem(color = MaterialTheme.colorScheme.primary, label = markedLabel)
            CalendarLegendItem(color = MaterialTheme.colorScheme.outline, label = todayLabel)
        }
    }
}

@Composable
private fun CalendarDayCell(day: CalendarDay) {
    Box(
        modifier =
            Modifier
                .size(MonthCalendarConstant.CELL_SIZE)
                .then(
                    if (day.isToday) {
                        Modifier.border(
                            MonthCalendarConstant.TODAY_BORDER_WIDTH,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(MonthCalendarConstant.CELL_CORNER_RADIUS),
                        )
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (day.dayOfMonth != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.isMarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier =
                        Modifier
                            .size(MonthCalendarConstant.MARK_DOT_SIZE)
                            .background(if (day.isMarked) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MonthCalendarConstant.LEGEND_DOT_TEXT_SPACING),
    ) {
        Box(modifier = Modifier.size(MonthCalendarConstant.LEGEND_DOT_SIZE).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
