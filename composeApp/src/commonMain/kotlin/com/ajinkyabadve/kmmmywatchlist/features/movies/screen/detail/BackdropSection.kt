package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import org.jetbrains.compose.resources.painterResource

@Composable
fun BackdropSection(detail: MovieDetail) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val backdropUrl =
        com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.resolve(
            path = detail.backdropPath,
            type = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.BACKDROP,
            targetWidthDp = 500,
            density = density,
        )
    val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val painter =
            rememberAsyncImagePainter(
                model = backdropUrl,
                filterQuality = FilterQuality.Medium,
                error = fallbackPainter,
                fallback = fallbackPainter,
            )
        val painterState by painter.state.collectAsState()
        val contentScale =
            if (painterState is AsyncImagePainter.State.Success) {
                ContentScale.Crop
            } else {
                ContentScale.Fit
            }

        Image(
            painter = painter,
            contentDescription = "Backdrop for ${detail.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )

        // Gradient overlay for visual contrast
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f,
                        ),
                    ),
        )

        // Trailer Play Button (overlaid in center of backdrop if youtube key is present)
        val youtubeTrailer =
            detail.videos?.results?.firstOrNull {
                it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true)
            }

        if (youtubeTrailer != null) {
            FloatingActionButton(
                onClick = {
                    val url = "https://www.youtube.com/watch?v=${youtubeTrailer.key}"
                    openUrl(url)
                },
                modifier = Modifier.align(Alignment.Center),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Trailer",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
