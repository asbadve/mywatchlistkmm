package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

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
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import org.jetbrains.compose.resources.painterResource

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

        if (imageUrl != null) {
            // Observe state so we switch from Fit → Crop once the image loads successfully
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

            Box(modifier.padding(8.dp)) {
                MediaCard(
                    modifier = modifier,
                    movieTitle = title,
                    painter = asyncPainter,
                    onClick = onClick,
                    contentScale = contentScale,
                )
            }
        } else {
            Box(modifier.padding(8.dp)) {
                MediaCard(
                    modifier = modifier,
                    movieTitle = title,
                    painter = fallbackPainter,
                    onClick = onClick,
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
