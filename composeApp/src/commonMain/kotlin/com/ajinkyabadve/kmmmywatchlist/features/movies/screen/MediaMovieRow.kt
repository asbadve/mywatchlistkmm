package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.design.movie.MediaCard
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.painterResource
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24

@Composable
fun mediaMovieRow(
    imageUrl: String?,
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
    isLoadingState: Boolean = false,
) {
    if (isLoadingState) {
        Box(
            modifier.padding(8.dp),
        ) {
            MediaCard(
                modifier = modifier,
                movieTitle = title,
                painter = null,
                onClick = onClick,
                isLoadingState = true,
            )
        }
    } else {
        val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)
        val painter = if (imageUrl != null) {
            rememberAsyncImagePainter(
                model = imageUrl,
                filterQuality = FilterQuality.Medium,
                error = fallbackPainter,
                fallback = fallbackPainter,
            )
        } else {
            fallbackPainter
        }
        Box(
            modifier.padding(8.dp),
        ) {
            MediaCard(
                modifier = modifier,
                movieTitle = title,
                painter = painter,
                onClick = onClick,
            )
        }
    }
}
