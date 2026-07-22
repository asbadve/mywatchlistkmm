package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.title_seasons
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSeasonsScreen(
    tvShowId: Long,
    onBackClicked: () -> Unit,
    onSeasonClicked: (Int) -> Unit,
    viewModel: AllSeasonsScreenModel = viewModel(key = "AllSeasonsScreenModel:$tvShowId") { AllSeasonsScreenModel(tvShowId) },
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.title_seasons), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                HorizontalDivider()
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is AllSeasonsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AllSeasonsState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadSeasons() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is AllSeasonsState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(state.seasons.sortedBy { it.seasonNumber }) { season ->
                            SeasonListItem(season = season, onClick = { onSeasonClicked(season.seasonNumber) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonListItem(
    season: SeasonSummary,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        val density = LocalDensity.current.density
        val posterUrl =
            ImageConfigResolver.resolve(
                path = season.posterPath,
                type = ImageConfigResolver.ImageType.POSTER,
                targetWidthDp = 100,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .width(90.dp)
                    .aspectRatio(2 / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_tv_24)
            val painter =
                rememberAsyncImagePainter(
                    model = posterUrl,
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
                contentDescription = season.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = season.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))

            val year = season.airDate?.takeIf { it.length >= 4 }?.take(4)
            val metaParts =
                buildList {
                    add("${season.episodeCount} Episodes")
                    year?.let { add(it) }
                }
            Text(
                text = metaParts.joinToString(" • "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            if (season.voteAverage > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${(season.voteAverage * 10).toInt() / 10.0} / 10",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = season.overview.ifEmpty { "No overview available for this season." },
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }
    }
}
