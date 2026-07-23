package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.youtubeUrl
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.filter_in_theaters
import mywatchlist.composeapp.generated.resources.filter_on_tv
import mywatchlist.composeapp.generated.resources.filter_popular
import mywatchlist.composeapp.generated.resources.filter_upcoming
import mywatchlist.composeapp.generated.resources.section_latest_trailers
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The Latest Trailers rail from the TMDB homepage: source filter chips plus wide video cards
// that open the trailer on YouTube.
@Composable
fun LatestTrailersSection(
    viewModel: TrendingScreenTabViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedSource by viewModel.selectedTrailerSource.collectAsState()
    val trailers by viewModel.trailerList.collectAsState()
    val isScreenLoading by viewModel.isTrailerScreenLoading.collectAsState()
    val isLoading by viewModel.isTrailerLoading.collectAsState()
    val error by viewModel.trailerError.collectAsState()

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, end = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.section_latest_trailers),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
        ) {
            TrailerSource.entries.forEach { source ->
                FilterChip(
                    selected = selectedSource == source,
                    onClick = { viewModel.onTrailerSourceSelected(source) },
                    label = { Text(stringResource(source.labelRes())) },
                )
            }
        }

        val currentError = error
        when {
            isScreenLoading || isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            currentError != null -> {
                Text(
                    text = currentError.asString(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                )
            }

            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    items(trailers, key = { it.video.id }) { trailer ->
                        TrailerCard(trailer = trailer)
                    }
                }
            }
        }
    }
}

private fun TrailerSource.labelRes(): StringResource =
    when (this) {
        TrailerSource.IN_THEATERS -> Res.string.filter_in_theaters
        TrailerSource.UPCOMING -> Res.string.filter_upcoming
        TrailerSource.POPULAR -> Res.string.filter_popular
        TrailerSource.ON_TV -> Res.string.filter_on_tv
    }

@Composable
private fun TrailerCard(trailer: Trailer) {
    Column(
        modifier =
            Modifier
                .width(280.dp)
                .clickable { openUrl(trailer.video.youtubeUrl()) },
    ) {
        val density = LocalDensity.current.density
        val backdropUrl =
            ImageConfigResolver.resolve(
                path = trailer.backdropPath,
                type = ImageConfigResolver.ImageType.BACKDROP,
                targetWidthDp = 280,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            backdropUrl?.let { url ->
                Image(
                    painter = rememberAsyncImagePainter(model = url),
                    contentDescription = trailer.mediaTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = trailer.video.name,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = trailer.mediaTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = trailer.video.name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}
