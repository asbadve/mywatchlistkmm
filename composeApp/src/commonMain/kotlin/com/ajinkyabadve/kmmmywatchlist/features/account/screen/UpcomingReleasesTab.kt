package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingMediaItem
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_upcoming_empty_message
import mywatchlist.composeapp.generated.resources.account_upcoming_empty_title
import mywatchlist.composeapp.generated.resources.upcoming_episode_label
import mywatchlist.composeapp.generated.resources.upcoming_in_days
import mywatchlist.composeapp.generated.resources.upcoming_today
import mywatchlist.composeapp.generated.resources.upcoming_tomorrow
import org.jetbrains.compose.resources.stringResource

private object UpcomingReleasesTabConstant {
    const val POSTER_TARGET_WIDTH_DP = 92
    val POSTER_WIDTH = 64.dp
    val POSTER_HEIGHT = 96.dp

    // POSTER_HEIGHT plus the row's own 4dp top/bottom padding - the row needs this as an explicit,
    // bounded height (not intrinsic) for TimelineIndicator's fillMaxHeight()/weight() to work.
    val ROW_HEIGHT = POSTER_HEIGHT + 8.dp
    val EMPTY_STATE_ICON_SIZE = 48.dp
    const val DAYS_UNTIL_PLAIN_DATE_FALLBACK = 7
    val TIMELINE_INDICATOR_WIDTH = 24.dp
    val TIMELINE_DOT_SIZE = 10.dp
    val TIMELINE_LINE_WIDTH = 2.dp
}

/**
 * MyFavTabs' "Upcoming" tab (future_features_checklist.md item 18) - a timeline of every tracked
 * movie not yet released and every unreleased episode of every tracked TV show (from cached season
 * data - see [UpcomingReleasesScreenModel]'s kdoc), grouped under one date header per group rather
 * than repeating the date on every row - [upcomingItems] is already date-sorted, so
 * [Iterable.groupBy] preserves that order into ascending-date groups for free. Purely local, no
 * network call, so there's no loading/error state to render - see `PersonFavoritesTab`'s identical
 * reasoning.
 */
@Composable
fun UpcomingReleasesTab(
    modifier: Modifier = Modifier,
    viewModel: UpcomingReleasesScreenModel = viewModel { UpcomingReleasesScreenModel() },
    lazyListState: LazyListState = rememberLazyListState(),
    onMovieSelected: (movieId: Long) -> Unit = {},
    onTvSelected: (tvId: Long) -> Unit = {},
) {
    val upcomingItems by viewModel.upcomingItems.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        if (upcomingItems.isEmpty()) {
            UpcomingReleasesEmptyState()
        } else {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val groupedByDate = upcomingItems.groupBy { it.date }
            LazyColumn(state = lazyListState) {
                groupedByDate.forEach { (date, itemsForDate) ->
                    item(key = "header:$date") {
                        UpcomingDateHeader(date = date, today = today)
                    }
                    itemsIndexed(
                        itemsForDate,
                        // Includes season/episode: a show with several unreleased episodes
                        // produces multiple rows sharing the same mediaType/id.
                        key = { _, item -> "${item.mediaType}:${item.id}:${item.seasonNumber}:${item.episodeNumber}" },
                    ) { index, item ->
                        UpcomingMediaRow(
                            item = item,
                            isFirstInGroup = index == 0,
                            isLastInGroup = index == itemsForDate.lastIndex,
                            onClick = {
                                if (item.mediaType == MediaTypeConstant.TV) {
                                    onTvSelected(item.id.toLong())
                                } else {
                                    onMovieSelected(item.id.toLong())
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingDateHeader(
    date: LocalDate,
    today: LocalDate,
) {
    Text(
        text = formatUpcomingDateLabel(date, today),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

/** The dot-and-line timeline rail to the left of a row - the line only spans between rows sharing
 *  the same date group, not above the group's first row or below its last. */
@Composable
private fun TimelineIndicator(
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
) {
    Column(
        modifier = Modifier.width(UpcomingReleasesTabConstant.TIMELINE_INDICATOR_WIDTH).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .width(UpcomingReleasesTabConstant.TIMELINE_LINE_WIDTH)
                    .background(if (isFirstInGroup) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier =
                Modifier
                    .size(UpcomingReleasesTabConstant.TIMELINE_DOT_SIZE)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .width(UpcomingReleasesTabConstant.TIMELINE_LINE_WIDTH)
                    .background(if (isLastInGroup) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun UpcomingMediaRow(
    item: UpcomingMediaItem,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current.density
    val imageUrl =
        ImageConfigResolver.resolve(
            path = item.posterPath,
            type = ImageConfigResolver.ImageType.POSTER,
            targetWidthDp = UpcomingReleasesTabConstant.POSTER_TARGET_WIDTH_DP,
            density = density,
        )

    Row(
        // A definite height here, not intrinsic, is required: TimelineIndicator's
        // fillMaxHeight()+weight() below need a bounded height constraint to distribute, which an
        // intrinsically-sized Row (one whose height is derived from its own children, including
        // the very child asking to fill it) cannot provide - crashed on real layout
        // (RowColumnMeasurePolicy/MeasurePassDelegate) despite passing Compose UI tests, since the
        // test harness's synthetic measurement didn't hit the same unbounded-constraint path a real
        // window does.
        modifier =
            Modifier
                .fillMaxWidth()
                .height(UpcomingReleasesTabConstant.ROW_HEIGHT)
                .clickable(onClick = onClick)
                .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineIndicator(isFirstInGroup = isFirstInGroup, isLastInGroup = isLastInGroup)
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            modifier =
                Modifier
                    // Modifier.size(dp) sets both dimensions - a trailing .size(POSTER_HEIGHT)
                    // after .width(POSTER_WIDTH) silently overrode the width, squaring the poster
                    // instead of the intended portrait shape.
                    .width(UpcomingReleasesTabConstant.POSTER_WIDTH)
                    .height(UpcomingReleasesTabConstant.POSTER_HEIGHT)
                    .clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleSmall)
            val seasonNumber = item.seasonNumber
            val episodeNumber = item.episodeNumber
            if (seasonNumber != null && episodeNumber != null) {
                Text(
                    text = stringResource(Res.string.upcoming_episode_label, seasonNumber, episodeNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Which relative-date label a header should show - a closed set resolved from a pure day-count
 *  comparison, kept separate from [formatUpcomingDateLabel]'s [stringResource] lookups so the
 *  resolution logic is unit-testable without a Compose test harness. */
internal sealed interface UpcomingDateLabel {
    data object Today : UpcomingDateLabel

    data object Tomorrow : UpcomingDateLabel

    data class InDays(
        val days: Int,
    ) : UpcomingDateLabel

    data class PlainDate(
        val date: LocalDate,
    ) : UpcomingDateLabel
}

internal fun resolveUpcomingDateLabel(
    date: LocalDate,
    today: LocalDate,
): UpcomingDateLabel {
    val daysUntil = today.daysUntil(date)
    return when {
        daysUntil == 0 -> UpcomingDateLabel.Today
        daysUntil == 1 -> UpcomingDateLabel.Tomorrow
        daysUntil in 2 until UpcomingReleasesTabConstant.DAYS_UNTIL_PLAIN_DATE_FALLBACK -> UpcomingDateLabel.InDays(daysUntil)
        else -> UpcomingDateLabel.PlainDate(date)
    }
}

@Composable
private fun formatUpcomingDateLabel(
    date: LocalDate,
    today: LocalDate,
): String =
    when (val label = resolveUpcomingDateLabel(date, today)) {
        UpcomingDateLabel.Today -> stringResource(Res.string.upcoming_today)
        UpcomingDateLabel.Tomorrow -> stringResource(Res.string.upcoming_tomorrow)
        is UpcomingDateLabel.InDays -> stringResource(Res.string.upcoming_in_days, label.days)
        is UpcomingDateLabel.PlainDate -> label.date.toString()
    }

@Composable
private fun UpcomingReleasesEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(UpcomingReleasesTabConstant.EMPTY_STATE_ICON_SIZE).padding(bottom = 12.dp),
        )
        Text(
            text = stringResource(Res.string.account_upcoming_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.account_upcoming_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
