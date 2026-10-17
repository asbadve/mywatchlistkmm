package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize

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
