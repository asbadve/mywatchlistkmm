package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.no_overview_available
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private object MediaListRowConstant {
    val POSTER_WIDTH = 80.dp
    const val POSTER_TARGET_WIDTH_DP = 90
}

/**
 * A movie/TV row (poster + title + year/rating + truncated overview) shared by every screen that
 * shows a plain vertical list of titles rather than a poster grid - `CollectionDetailScreen` and
 * `ListDetailScreen` both render their items with this, instead of each keeping its own near-
 * identical copy.
 */
@Composable
fun MediaListRow(
    title: String,
    posterPath: String?,
    yearAndRating: String?,
    overview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val density = LocalDensity.current.density
        val posterUrl =
            ImageConfigResolver.resolve(
                path = posterPath,
                type = ImageConfigResolver.ImageType.POSTER,
                targetWidthDp = MediaListRowConstant.POSTER_TARGET_WIDTH_DP,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .width(MediaListRowConstant.POSTER_WIDTH)
                    .aspectRatio(2 / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)
            val painter =
                rememberAsyncImagePainter(
                    model = posterUrl,
                    filterQuality = FilterQuality.Medium,
                    error = fallbackPainter,
                    fallback = fallbackPainter,
                )
            val painterState by painter.state.collectAsState()
            val contentScale =
                if (painterState is AsyncImagePainter.State.Success) ContentScale.Crop else ContentScale.Fit
            Image(
                painter = painter,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!yearAndRating.isNullOrEmpty()) {
                Text(
                    text = yearAndRating,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = overview.ifEmpty { stringResource(Res.string.no_overview_available) },
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        trailingContent?.invoke()
    }
}
