package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.design.movie.MediaCard
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import org.jetbrains.compose.resources.painterResource

/**
 * A search result poster/headshot. Mirrors `mediaMovieRow`, but kept separate because search cards
 * are stacked under a [MediaTypeBadge] overlay and carry a year caption, which the plain media rows
 * don't.
 */
@Composable
fun searchMediaRow(
    imageUrl: String?,
    title: String,
    mediaType: SearchMediaType?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    // TMDB has no image for plenty of lesser-known people and shows, and a film clapperboard
    // standing in for a missing headshot reads as a wrong result rather than a missing photo.
    val fallbackPainter =
        painterResource(
            when (mediaType) {
                SearchMediaType.PERSON -> Res.drawable.baseline_person_24
                SearchMediaType.TV -> Res.drawable.baseline_tv_24
                SearchMediaType.MOVIE, null -> Res.drawable.baseline_movie_24
            },
        )

    if (imageUrl == null) {
        Box(modifier.padding(CARD_PADDING)) {
            MediaCard(
                modifier = modifier,
                movieTitle = title,
                painter = fallbackPainter,
                onClick = onClick,
                contentScale = ContentScale.Fit,
            )
        }
        return
    }

    // Observe state so we switch from Fit → Crop once the image loads successfully.
    val asyncPainter =
        rememberAsyncImagePainter(
            model = imageUrl,
            filterQuality = FilterQuality.Medium,
            error = fallbackPainter,
            fallback = fallbackPainter,
        )
    val painterState by asyncPainter.state.collectAsState()
    val contentScale =
        if (painterState is AsyncImagePainter.State.Success) ContentScale.Crop else ContentScale.Fit

    Box(modifier.padding(CARD_PADDING)) {
        MediaCard(
            modifier = modifier,
            movieTitle = title,
            painter = asyncPainter,
            onClick = onClick,
            contentScale = contentScale,
        )
    }
}

private val CARD_PADDING = 8.dp
