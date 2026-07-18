package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.CastSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieImagesSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.OverviewSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.VideoClipsSection
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    tvShowId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    viewModel: EpisodeDetailScreenModel =
        viewModel(key = "EpisodeDetailScreenModel:$tvShowId:$seasonNumber:$episodeNumber") {
            EpisodeDetailScreenModel(tvShowId, seasonNumber, episodeNumber)
        },
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        val title = (uiState as? EpisodeDetailState.Success)?.episode?.name ?: "Episode"
                        Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is EpisodeDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EpisodeDetailState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadEpisodeDetails() }) {
                            Text("Retry")
                        }
                    }
                }

                is EpisodeDetailState.Success -> {
                    if (windowSize.isCompact()) {
                        CompactEpisodeDetailContent(
                            episode = state.episode,
                            onShowGallery = { images, index ->
                                galleryImages = images
                                galleryInitialIndex = index
                            },
                        )
                    } else {
                        ExpandedEpisodeDetailContent(
                            episode = state.episode,
                            onShowGallery = { images, index ->
                                galleryImages = images
                                galleryInitialIndex = index
                            },
                        )
                    }
                }
            }

            galleryImages?.let { images ->
                FullscreenMediaGallery(
                    images = images,
                    initialIndex = galleryInitialIndex,
                    onDismiss = { galleryImages = null },
                    onDownload = { imageUrl -> ImageDownloader.downloadAndSave(imageUrl) },
                )
            }
        }
    }
}

@Composable
private fun CompactEpisodeDetailContent(
    episode: EpisodeDetail,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { EpisodeStillHeader(episode = episode) }
        item { OverviewSection(overview = episode.overview) }
        item { VideoClipsSection(videos = episode.videos?.results ?: emptyList()) }
        item {
            MovieImagesSection(
                images = episode.images?.stills ?: emptyList(),
                title = "Images",
                imageType = ImageConfigResolver.ImageType.STILL,
                onShowGallery = onShowGallery,
            )
        }
        item {
            CastSection(castList = episode.credits?.guestStars ?: emptyList(), title = "Guest Stars")
        }
    }
}

@Composable
private fun ExpandedEpisodeDetailContent(
    episode: EpisodeDetail,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1.5f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { EpisodeStillHeader(episode = episode) }
            item { OverviewSection(overview = episode.overview) }
            item { VideoClipsSection(videos = episode.videos?.results ?: emptyList()) }
            item {
                MovieImagesSection(
                    images = episode.images?.stills ?: emptyList(),
                    title = "Images",
                    imageType = ImageConfigResolver.ImageType.STILL,
                    onShowGallery = onShowGallery,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp),
        ) {
            item {
                CastSection(castList = episode.credits?.guestStars ?: emptyList(), title = "Guest Stars")
            }
        }
    }
}

@Composable
private fun EpisodeStillHeader(episode: EpisodeDetail) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        val density = LocalDensity.current.density
        val stillUrl = ImageConfigResolver.resolve(
            path = episode.stillPath,
            type = ImageConfigResolver.ImageType.STILL,
            targetWidthDp = 600,
            density = density
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = "Episode ${episode.episodeNumber} • Season ${episode.seasonNumber}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            if (!episode.airDate.isNullOrEmpty()) {
                Text(
                    text = episode.airDate,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            episode.runtime?.let { runtime ->
                Text(
                    text = "$runtime min",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            if (episode.voteAverage > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(episode.voteAverage * 10).toInt() / 10.0} / 10",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
