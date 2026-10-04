package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.rememberNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.design.calendar.MonthCalendarCard
import com.ajinkyabadve.kmmmywatchlist.design.calendar.monthNameRes
import com.ajinkyabadve.kmmmywatchlist.design.calendar.weekdayNameRes
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.design.pill.StatusPill
import com.ajinkyabadve.kmmmywatchlist.design.timeline.TimelineEntryCard
import com.ajinkyabadve.kmmmywatchlist.design.timeline.TimelineSectionHeader
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.formatFullReleaseDate
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.MediaTypeBadge
import com.ajinkyabadve.kmmmywatchlist.isMobilePlatform
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_upcoming_empty_message
import mywatchlist.composeapp.generated.resources.account_upcoming_empty_title
import mywatchlist.composeapp.generated.resources.action_reminder_set
import mywatchlist.composeapp.generated.resources.upcoming_calendar_legend_release
import mywatchlist.composeapp.generated.resources.upcoming_calendar_legend_today
import mywatchlist.composeapp.generated.resources.upcoming_episode_label
import mywatchlist.composeapp.generated.resources.upcoming_filter_all
import mywatchlist.composeapp.generated.resources.upcoming_filter_movie
import mywatchlist.composeapp.generated.resources.upcoming_filter_tv
import mywatchlist.composeapp.generated.resources.upcoming_in_days
import mywatchlist.composeapp.generated.resources.upcoming_next_up_label
import mywatchlist.composeapp.generated.resources.upcoming_remind_me_button
import mywatchlist.composeapp.generated.resources.upcoming_remind_me_content_description
import mywatchlist.composeapp.generated.resources.upcoming_reminder_set_content_description
import mywatchlist.composeapp.generated.resources.upcoming_today
import mywatchlist.composeapp.generated.resources.upcoming_tomorrow
import mywatchlist.composeapp.generated.resources.upcoming_view_details_button
import org.jetbrains.compose.resources.stringResource

private object UpcomingReleasesTabConstant {
    const val NEXT_UP_TITLE_MAX_LINES = 2

    val EMPTY_STATE_ICON_SIZE = 48.dp
    const val DAYS_UNTIL_PLAIN_DATE_FALLBACK = 7
    val WEEKDAY_COLUMN_WIDTH = 44.dp
    const val ROW_TITLE_MAX_LINES = 2
    const val HEADER_ITEM_KEY = "header"

    /** formatFullReleaseDate's "Oct 14, 2026" splits into day and year here. */
    const val FULL_DATE_YEAR_SEPARATOR = ","

    val RIGHT_COLUMN_MAX_WIDTH = 360.dp
    const val TIMELINE_WEIGHT = 1.45f
    const val RIGHT_COLUMN_WEIGHT = 1f
    const val NEXT_UP_GRADIENT_ALPHA = 0.35f
}

/**
 * Poster sizes and row shape per [WindowSize]. A phone keeps small posters and stacks each row's
 * badge, title and countdown pill in one column, so the title isn't squeezed. Wider windows have
 * room for bigger posters and the pill beside the title. Every poster is 2:3; [rowTargetWidthDp]
 * and [nextUpTargetWidthDp] pick the TMDB image size to download.
 */
internal data class UpcomingLayoutSizes(
    val rowPosterWidth: Dp,
    val rowPosterHeight: Dp,
    val rowTargetWidthDp: Int,
    val nextUpPosterWidth: Dp,
    val nextUpPosterHeight: Dp,
    val nextUpTargetWidthDp: Int,
    val stackedRows: Boolean,
) {
    /** A stacked row (badge+episode, title, pill) needs slightly more height than a phone's
     *  poster alone; a side-by-side row is exactly as tall as its poster. */
    val rowContentHeight: Dp
        get() = if (stackedRows) maxOf(rowPosterHeight, STACKED_ROW_MIN_HEIGHT) else rowPosterHeight

    companion object {
        private val STACKED_ROW_MIN_HEIGHT = 104.dp

        // Phone: the Next-up poster sits between the grid's 150dp and a row's - the full grid
        // size made the card take most of a small screen.
        private val COMPACT =
            UpcomingLayoutSizes(
                rowPosterWidth = 64.dp,
                rowPosterHeight = 96.dp,
                rowTargetWidthDp = 92,
                nextUpPosterWidth = 96.dp,
                nextUpPosterHeight = 144.dp,
                nextUpTargetWidthDp = 96,
                stackedRows = true,
            )

        // Unfolded foldable / small tablet: still a single column, but wide enough for bigger
        // posters and the countdown pill beside the title.
        private val MEDIUM =
            UpcomingLayoutSizes(
                rowPosterWidth = 80.dp,
                rowPosterHeight = 120.dp,
                rowTargetWidthDp = 120,
                nextUpPosterWidth = 128.dp,
                nextUpPosterHeight = 192.dp,
                nextUpTargetWidthDp = 150,
                stackedRows = false,
            )

        // Tablet / desktop: the Next-up spotlight matches the Favorites/Watchlist grid's 150dp
        // poster (AccountMediaGridConstant); rows stay smaller so the spotlight stays the focus.
        private val EXPANDED =
            UpcomingLayoutSizes(
                rowPosterWidth = 88.dp,
                rowPosterHeight = 132.dp,
                rowTargetWidthDp = 132,
                nextUpPosterWidth = 150.dp,
                nextUpPosterHeight = 225.dp,
                nextUpTargetWidthDp = 150,
                stackedRows = false,
            )

        fun forWindowSize(windowSize: WindowSize): UpcomingLayoutSizes =
            when (windowSize) {
                WindowSize.COMPACT -> COMPACT
                WindowSize.MEDIUM -> MEDIUM
                WindowSize.EXPANDED -> EXPANDED
            }
    }
}

/** All / Movie / TV - a closed set, driving [scrollableChips] over the already-loaded list. */
internal enum class UpcomingFilter {
    ALL,
    MOVIE,
    TV,
}

/**
 * MyFavTabs' "Upcoming" tab (future_features_checklist.md item 18/15) - a timeline of every
 * tracked movie not yet released and every unreleased episode of every tracked TV show (from
 * cached season data - see [UpcomingReleasesScreenModel]'s kdoc), grouped by month with one header
 * per month rather than repeating it on every row - [upcomingItems] is already date-sorted, so
 * [Iterable.groupBy] preserves that order into ascending-month groups for free.
 *
 * On an expanded [windowSize] (desktop/tablet-landscape width), the extra space is used for a
 * "Next up" spotlight card and a release calendar alongside the timeline, per the Claude Design
 * mockup this screen implements - on compact/medium widths those collapse into a single scrolling
 * column with a compact "Next up" card above the timeline. Purely local, no network call, so
 * there's no loading/error state to render - see `PersonFavoritesTab`'s identical reasoning.
 */
@Composable
fun UpcomingReleasesTab(
    windowSize: WindowSize,
    modifier: Modifier = Modifier,
    viewModel: UpcomingReleasesScreenModel = viewModel { UpcomingReleasesScreenModel() },
    lazyListState: LazyListState = rememberLazyListState(),
    onMovieSelected: (movieId: Long) -> Unit = {},
    onTvSelected: (tvId: Long) -> Unit = {},
    // A scheduled reminder can only be delivered on Android/iOS - desktop/browser have no OS
    // scheduler that fires with the app closed, so the reminder UI (item 16) never renders there.
    // A parameter rather than a direct call so the UI test (which runs on desktop) can render it.
    showReminderControls: Boolean = isMobilePlatform(),
) {
    val upcomingItems by viewModel.upcomingItems.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (upcomingItems.isEmpty()) {
            UpcomingReleasesEmptyState()
        } else {
            var selectedFilter by remember { mutableStateOf(UpcomingFilter.ALL) }
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val nextUpItem = remember(upcomingItems) { upcomingItems.minByOrNull { it.date } }
            val filteredItems =
                remember(upcomingItems, selectedFilter) {
                    when (selectedFilter) {
                        UpcomingFilter.ALL -> upcomingItems
                        UpcomingFilter.MOVIE -> upcomingItems.filter { it.mediaType == MediaTypeConstant.MOVIE }
                        UpcomingFilter.TV -> upcomingItems.filter { it.mediaType == MediaTypeConstant.TV }
                    }
                }
            val groupedByMonth = remember(filteredItems) { filteredItems.groupBy { monthGroupKey(it.date) } }
            val onItemClick: (UpcomingMediaItem) -> Unit = { item ->
                if (item.mediaType == MediaTypeConstant.TV) onTvSelected(item.id.toLong()) else onMovieSelected(item.id.toLong())
            }
            // The reminder flow is only collected behind the platform gate, so desktop never reads
            // the reminder table.
            val remindedKeys =
                if (showReminderControls) {
                    viewModel.remindedKeys.collectAsState(initial = emptySet()).value
                } else {
                    emptySet()
                }
            val permissionRequester = rememberNotificationPermissionRequester()
            val reminderScope = rememberCoroutineScope()
            val onToggleReminder: (UpcomingMediaItem) -> Unit = { item ->
                if (item.reminderKey() in remindedKeys) {
                    viewModel.setReminder(item, enabled = false)
                } else {
                    reminderScope.launch {
                        if (permissionRequester.request()) viewModel.setReminder(item, enabled = true)
                    }
                }
            }

            val sizes = UpcomingLayoutSizes.forWindowSize(windowSize)
            if (windowSize.isExpanded()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(UpcomingReleasesTabConstant.TIMELINE_WEIGHT).fillMaxHeight()) {
                        UpcomingFilterChips(selectedFilter = selectedFilter, onSelect = { selectedFilter = it })
                        UpcomingTimelineList(
                            groupedByMonth = groupedByMonth,
                            today = today,
                            nextUpItem = nextUpItem,
                            lazyListState = lazyListState,
                            showBell = showReminderControls,
                            remindedKeys = remindedKeys,
                            onToggleReminder = onToggleReminder,
                            sizes = sizes,
                            modifier = Modifier.weight(1f),
                            onMovieSelected = onMovieSelected,
                            onTvSelected = onTvSelected,
                        )
                    }
                    Column(
                        modifier =
                            Modifier
                                .weight(UpcomingReleasesTabConstant.RIGHT_COLUMN_WEIGHT)
                                .widthIn(max = UpcomingReleasesTabConstant.RIGHT_COLUMN_MAX_WIDTH)
                                .fillMaxHeight()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        nextUpItem?.let { item ->
                            NextUpCard(
                                item = item,
                                today = today,
                                expanded = true,
                                sizes = sizes,
                                showReminderControls = showReminderControls,
                                isReminderSet = item.reminderKey() in remindedKeys,
                                onToggleReminder = { onToggleReminder(item) },
                                onClick = { onItemClick(item) },
                            )
                        }
                        UpcomingCalendarCard(upcomingItems = upcomingItems, today = today)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    UpcomingTimelineList(
                        // Scrolls away with the rows rather than staying pinned above them: on a
                        // small phone a pinned card + chips left room for about one and a half rows.
                        header = {
                            nextUpItem?.let { item ->
                                NextUpCard(
                                    item = item,
                                    today = today,
                                    expanded = false,
                                    sizes = sizes,
                                    showReminderControls = showReminderControls,
                                    isReminderSet = item.reminderKey() in remindedKeys,
                                    onToggleReminder = { onToggleReminder(item) },
                                    onClick = { onItemClick(item) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                            UpcomingFilterChips(selectedFilter = selectedFilter, onSelect = { selectedFilter = it })
                        },
                        groupedByMonth = groupedByMonth,
                        today = today,
                        nextUpItem = nextUpItem,
                        lazyListState = lazyListState,
                        showBell = showReminderControls,
                        remindedKeys = remindedKeys,
                        onToggleReminder = onToggleReminder,
                        sizes = sizes,
                        modifier = Modifier.weight(1f),
                        onMovieSelected = onMovieSelected,
                        onTvSelected = onTvSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingFilterChips(
    selectedFilter: UpcomingFilter,
    onSelect: (UpcomingFilter) -> Unit,
) {
    val filters = remember { UpcomingFilter.entries }
    val labels =
        filters.map { filter ->
            when (filter) {
                UpcomingFilter.ALL -> stringResource(Res.string.upcoming_filter_all)
                UpcomingFilter.MOVIE -> stringResource(Res.string.upcoming_filter_movie)
                UpcomingFilter.TV -> stringResource(Res.string.upcoming_filter_tv)
            }
        }
    scrollableChips(
        selectedChip = filters.indexOf(selectedFilter),
        chipItemList = labels,
        onClick = { index -> onSelect(filters[index]) },
    )
}

@Composable
private fun UpcomingTimelineList(
    groupedByMonth: Map<Pair<Int, Int>, List<UpcomingMediaItem>>,
    today: LocalDate,
    nextUpItem: UpcomingMediaItem?,
    lazyListState: LazyListState,
    showBell: Boolean,
    remindedKeys: Set<ReminderKey>,
    onToggleReminder: (UpcomingMediaItem) -> Unit,
    sizes: UpcomingLayoutSizes,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    onMovieSelected: (Long) -> Unit = {},
    onTvSelected: (Long) -> Unit = {},
) {
    LazyColumn(state = lazyListState, modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            item(key = UpcomingReleasesTabConstant.HEADER_ITEM_KEY) { Column { header() } }
        }
        groupedByMonth.forEach { (monthKey, itemsForMonth) ->
            item(key = "month:${monthKey.first}-${monthKey.second}") {
                UpcomingMonthHeader(year = monthKey.first, monthNumber = monthKey.second)
            }
            items(
                itemsForMonth,
                // Includes season/episode: a show with several unreleased episodes
                // produces multiple rows sharing the same mediaType/id.
                key = { item -> "${item.mediaType}:${item.id}:${item.seasonNumber}:${item.episodeNumber}" },
            ) { item ->
                UpcomingMediaRow(
                    item = item,
                    today = today,
                    isNextUp = item == nextUpItem,
                    showBell = showBell,
                    isReminderSet = item.reminderKey() in remindedKeys,
                    onToggleReminder = { onToggleReminder(item) },
                    sizes = sizes,
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

@Composable
private fun UpcomingMonthHeader(
    year: Int,
    monthNumber: Int,
) {
    TimelineSectionHeader(title = "${stringResource(monthNameRes(monthNumber))} $year")
}

@Composable
private fun UpcomingMediaRow(
    item: UpcomingMediaItem,
    today: LocalDate,
    isNextUp: Boolean,
    showBell: Boolean,
    isReminderSet: Boolean,
    onToggleReminder: () -> Unit,
    sizes: UpcomingLayoutSizes,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current.density
    val imageUrl =
        ImageConfigResolver.resolve(
            path = item.posterPath,
            type = ImageConfigResolver.ImageType.POSTER,
            targetWidthDp = sizes.rowTargetWidthDp,
            density = density,
        )
    TimelineEntryCard(
        contentHeight = sizes.rowContentHeight,
        highlighted = isNextUp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.width(UpcomingReleasesTabConstant.WEEKDAY_COLUMN_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(weekdayNameRes(item.date.dayOfWeek)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    item.date.dayOfMonth
                        .toString()
                        .padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            modifier =
                Modifier
                    .padding(start = 12.dp)
                    // Modifier.size(dp) sets both dimensions - a trailing .size(POSTER_HEIGHT)
                    // after .width(POSTER_WIDTH) silently overrode the width, squaring the poster
                    // instead of the intended portrait shape.
                    .width(sizes.rowPosterWidth)
                    .height(sizes.rowPosterHeight)
                    .clip(RoundedCornerShape(8.dp)),
        )
        val seasonNumber = item.seasonNumber
        val episodeNumber = item.episodeNumber
        val episodeLabel =
            if (seasonNumber != null && episodeNumber != null) {
                stringResource(Res.string.upcoming_episode_label, seasonNumber, episodeNumber)
            } else {
                null
            }
        if (sizes.stackedRows) {
            // A phone is too narrow for poster + title + a trailing pill + bell side by side (the
            // title column shrank to one letter per line), so the pill moves under the title and
            // the episode label beside the badge.
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaTypeBadge(mediaType = item.toSearchMediaType())
                    if (episodeLabel != null) {
                        Text(
                            text = episodeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = UpcomingReleasesTabConstant.ROW_TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                StatusPill(text = formatUpcomingDateLabel(item.date, today), highlighted = isNextUp)
            }
        } else {
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                MediaTypeBadge(mediaType = item.toSearchMediaType(), modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = UpcomingReleasesTabConstant.ROW_TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episodeLabel != null) {
                    Text(
                        text = episodeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            UpcomingCountdownPill(date = item.date, today = today, highlighted = isNextUp)
        }
        if (showBell) {
            ReminderBellButton(isSet = isReminderSet, emphasized = isNextUp, onClick = onToggleReminder)
        }
    }
}

/** The bell on a timeline row and the compact Next-up card: filled once a reminder is set,
 *  outlined otherwise. [emphasized] tints it with the primary colour even before it's set. */
@Composable
private fun ReminderBellButton(
    isSet: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isSet) Icons.Filled.Notifications else Icons.Outlined.Notifications,
            contentDescription =
                stringResource(
                    if (isSet) Res.string.upcoming_reminder_set_content_description else Res.string.upcoming_remind_me_content_description,
                ),
            tint = if (isSet || emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpcomingCountdownPill(
    date: LocalDate,
    today: LocalDate,
    highlighted: Boolean,
) {
    StatusPill(
        text = formatUpcomingDateLabel(date, today),
        highlighted = highlighted,
        modifier = Modifier.padding(start = 8.dp),
    )
}

/** The "what's releasing next" focal point - a bigger spotlight card with action buttons on an
 *  expanded [windowSize] (desktop right column), a compact single-row card otherwise (top of the
 *  mobile timeline). Reuses a gradient [Brush] scrim rather than [Modifier.blur] - this codebase
 *  has no existing blur usage and the gradient achieves the same "moody backdrop" effect using the
 *  same primitive `BackdropSection`/`HeroScrimStops` already rely on. */
@Composable
private fun NextUpCard(
    item: UpcomingMediaItem,
    today: LocalDate,
    expanded: Boolean,
    sizes: UpcomingLayoutSizes,
    showReminderControls: Boolean,
    isReminderSet: Boolean,
    onToggleReminder: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val imageUrl =
        ImageConfigResolver.resolve(
            path = item.posterPath,
            type = ImageConfigResolver.ImageType.POSTER,
            targetWidthDp = sizes.nextUpTargetWidthDp,
            density = density,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = UpcomingReleasesTabConstant.NEXT_UP_GRADIENT_ALPHA),
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ),
                ).clickable(onClick = onClick)
                .padding(if (expanded) 16.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier =
                    Modifier
                        .width(sizes.nextUpPosterWidth)
                        .height(sizes.nextUpPosterHeight)
                        .clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = stringResource(Res.string.upcoming_next_up_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = item.title,
                    style = if (expanded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    maxLines = UpcomingReleasesTabConstant.NEXT_UP_TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                val seasonNumber = item.seasonNumber
                val episodeNumber = item.episodeNumber
                if (seasonNumber != null && episodeNumber != null) {
                    Text(
                        text = stringResource(Res.string.upcoming_episode_label, seasonNumber, episodeNumber),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // The compact card's bell sits on the date's line rather than in a trailing column of
                // its own, which would take width the title needs on a phone.
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatUpcomingDateLabel(item.date, today),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!expanded && showReminderControls) {
                        ReminderBellButton(isSet = isReminderSet, emphasized = true, onClick = onToggleReminder)
                    }
                }
            }
        }
        if (expanded) {
            // Below the poster at the card's full width, not in the text column beside it: next
            // to a 150dp poster that column is too narrow for both buttons at the smallest
            // expanded width, and their labels were cut off.
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onClick) {
                    Text(stringResource(Res.string.upcoming_view_details_button), maxLines = 1)
                }
                if (showReminderControls) {
                    OutlinedButton(onClick = onToggleReminder) {
                        Text(
                            stringResource(if (isReminderSet) Res.string.action_reminder_set else Res.string.upcoming_remind_me_button),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Desktop-only release calendar for the current month - static (no prev/next navigation) by
 *  deliberate initial scope cut; see the Upcoming Timeline redesign plan. */
@Composable
private fun UpcomingCalendarCard(
    upcomingItems: List<UpcomingMediaItem>,
    today: LocalDate,
) {
    val releaseDays =
        remember(upcomingItems, today) {
            upcomingItems
                .filter { it.date.year == today.year && it.date.monthNumber == today.monthNumber }
                .map { it.date.dayOfMonth }
                .toSet()
        }
    MonthCalendarCard(
        year = today.year,
        monthNumber = today.monthNumber,
        markedDays = releaseDays,
        today = today,
        markedLabel = stringResource(Res.string.upcoming_calendar_legend_release),
        todayLabel = stringResource(Res.string.upcoming_calendar_legend_today),
    )
}

private fun UpcomingMediaItem.toSearchMediaType(): SearchMediaType =
    if (mediaType == MediaTypeConstant.TV) SearchMediaType.TV else SearchMediaType.MOVIE

/** (year, monthNumber) grouping key - pure so the grouping itself is unit-testable without a
 *  Compose test harness. [upcomingItems] is already date-sorted, so grouping preserves ascending
 *  month order for free. */
internal fun monthGroupKey(date: LocalDate): Pair<Int, Int> = date.year to date.monthNumber

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
        // "Oct 14", plus the year only when it isn't this year - the raw ISO date was too wide for
        // a phone row's pill and wrapped onto two lines.
        is UpcomingDateLabel.PlainDate -> {
            val full = formatFullReleaseDate(label.date.toString())
            if (label.date.year == today.year) full.substringBefore(UpcomingReleasesTabConstant.FULL_DATE_YEAR_SEPARATOR) else full
        }
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
