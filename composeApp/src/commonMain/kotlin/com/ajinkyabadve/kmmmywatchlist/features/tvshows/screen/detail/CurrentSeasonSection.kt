package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.SectionHeaderWithScrollHint
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import org.jetbrains.compose.resources.painterResource

@Composable
fun CurrentSeasonSection(
    season: TvSeasonDetail,
    onViewAllSeasonsClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Season",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onViewAllSeasonsClick) {
                Text("View All")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            val posterUrl = ImageConfigResolver.resolve(
                path = season.posterPath,
                type = ImageConfigResolver.ImageType.POSTER,
                targetWidthDp = 100,
                density = density
            )
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(2 / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val fallbackPainter = painterResource(Res.drawable.baseline_tv_24)
                val painter = rememberAsyncImagePainter(
                    model = posterUrl,
                    filterQuality = FilterQuality.Medium,
                    error = fallbackPainter,
                    fallback = fallbackPainter
                )
                val painterState by painter.state.collectAsState()
                val contentScale = if (painterState is AsyncImagePainter.State.Success) {
                    ContentScale.Crop
                } else {
                    ContentScale.Fit
                }
                Image(
                    painter = painter,
                    contentDescription = season.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = season.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!season.airDate.isNullOrEmpty()) {
                    Text(
                        text = season.airDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = season.overview.ifEmpty { "No overview available for this season." },
                    fontSize = 13.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        if (season.episodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            val lazyRowState = rememberLazyListState()
            SectionHeaderWithScrollHint(
                title = "Episodes",
                listSize = season.episodes.size,
                lazyRowState = lazyRowState,
                scrollStep = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(season.episodes) { episode ->
                    EpisodeCard(episode = episode)
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val stillUrl = ImageConfigResolver.resolve(
        path = episode.stillPath,
        type = ImageConfigResolver.ImageType.STILL,
        targetWidthDp = 200,
        density = density
    )

    Column(modifier = Modifier.width(180.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f),
            shape = RoundedCornerShape(8.dp)
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_tv_24)
            val painter = rememberAsyncImagePainter(
                model = stillUrl,
                filterQuality = FilterQuality.Medium,
                error = fallbackPainter,
                fallback = fallbackPainter
            )
            val painterState by painter.state.collectAsState()
            val contentScale = if (painterState is AsyncImagePainter.State.Success) {
                ContentScale.Crop
            } else {
                ContentScale.Fit
            }
            Image(
                painter = painter,
                contentDescription = episode.name,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = contentScale
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${episode.episodeNumber}. ${episode.name}",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!episode.airDate.isNullOrEmpty()) {
            Text(
                text = episode.airDate,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
