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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.CastSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.DirectedByLine
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieImagesSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.OverviewSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.VideoClipsSection
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.openUrl
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.action_view_on_imdb
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.label_production_code
import mywatchlist.composeapp.generated.resources.label_written_by
import mywatchlist.composeapp.generated.resources.section_cast
import mywatchlist.composeapp.generated.resources.section_guest_stars
import mywatchlist.composeapp.generated.resources.section_images
import mywatchlist.composeapp.generated.resources.title_episode
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    tvShowId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onPersonClicked: (Long) -> Unit = {},
    viewModel: EpisodeDetailScreenModel =
        viewModel(key = "EpisodeDetailScreenModel:$tvShowId:$seasonNumber:$episodeNumber") {
            EpisodeDetailScreenModel(tvShowId, seasonNumber, episodeNumber)
        },
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()

    // Hide-on-scroll-down / reveal-on-scroll-up, matching the app's collapsing bottom nav.
    //
    // `canScroll` is not optional here. Left at its default of `{ true }`, Material3 moves the bar
    // for any drag, including on an episode whose content fits the screen - the bar would leave on
    // a screen that never scrolled, taking the only back affordance with it. Tying it to the list
    // means a list with nowhere to go keeps its bar. See CollapsibleBarState for the same fix on
    // the bars this app drives itself.
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            canScroll = { listState.canScrollForward || listState.canScrollBackward },
        )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                title = (uiState as? EpisodeDetailState.Success)?.episode?.name ?: stringResource(Res.string.title_episode),
                onBackClicked = onBackClicked,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is EpisodeDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EpisodeDetailState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadEpisodeDetails() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is EpisodeDetailState.Success -> {
                    if (windowSize.isCompact()) {
                        CompactEpisodeDetailContent(
                            episode = state.episode,
                            listState = listState,
                            onPersonClicked = onPersonClicked,
                            onShowGallery = { images, index ->
                                galleryImages = images
                                galleryInitialIndex = index
                            },
                        )
                    } else {
                        ExpandedEpisodeDetailContent(
                            episode = state.episode,
                            onPersonClicked = onPersonClicked,
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
    listState: LazyListState,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        // Hoisted so the top bar's `canScroll` can ask whether this list has anywhere to go.
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { EpisodeStillHeader(episode = episode, onPersonClicked = onPersonClicked) }
        item { OverviewSection(overview = episode.overview) }
        item { VideoClipsSection(videos = episode.videos?.results ?: emptyList()) }
        item {
            MovieImagesSection(
                images = episode.images?.stills ?: emptyList(),
                title = stringResource(Res.string.section_images),
                imageType = ImageConfigResolver.ImageType.STILL,
                onShowGallery = onShowGallery,
            )
        }
        item {
            CastSection(
                castList = episode.credits?.cast ?: emptyList(),
                title = stringResource(Res.string.section_cast),
                onPersonClicked = onPersonClicked,
            )
        }
        item {
            CastSection(
                castList = episode.credits?.guestStars ?: emptyList(),
                title = stringResource(Res.string.section_guest_stars),
                onPersonClicked = onPersonClicked,
            )
        }
    }
}

@Composable
private fun ExpandedEpisodeDetailContent(
    episode: EpisodeDetail,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { EpisodeStillHeader(episode = episode, onPersonClicked = onPersonClicked) }
            item { OverviewSection(overview = episode.overview) }
            item { VideoClipsSection(videos = episode.videos?.results ?: emptyList()) }
            item {
                MovieImagesSection(
                    images = episode.images?.stills ?: emptyList(),
                    title = stringResource(Res.string.section_images),
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
                CastSection(
                    castList = episode.credits?.cast ?: emptyList(),
                    title = stringResource(Res.string.section_cast),
                    onPersonClicked = onPersonClicked,
                )
            }
            item {
                CastSection(
                    castList = episode.credits?.guestStars ?: emptyList(),
                    title = stringResource(Res.string.section_guest_stars),
                    onPersonClicked = onPersonClicked,
                )
            }
        }
    }
}

@Composable
private fun EpisodeStillHeader(
    episode: EpisodeDetail,
    onPersonClicked: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        val density = LocalDensity.current.density
        val stillUrl =
            ImageConfigResolver.resolve(
                path = episode.stillPath,
                type = ImageConfigResolver.ImageType.STILL,
                targetWidthDp = 600,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_tv_24)
            val painter =
                rememberAsyncImagePainter(
                    model = stillUrl,
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
                contentDescription = episode.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Episode ${episode.episodeNumber} • Season ${episode.seasonNumber}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            // "standard" is the default episode type - only special ones (finale, mid_season)
            // are worth a badge.
            episode.episodeType?.takeIf { it.isNotEmpty() && it != "standard" }?.let { type ->
                Text(
                    text = formatEpisodeType(type),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (!episode.airDate.isNullOrEmpty()) {
                Text(
                    text = episode.airDate,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            episode.runtime?.let { runtime ->
                Text(
                    text = "$runtime min",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            if (episode.voteAverage > 0) {
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
                        text =
                            "${(episode.voteAverage * 10).toInt() / 10.0} / 10" +
                                if (episode.voteCount > 0) " (${episode.voteCount} votes)" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    )
                }
            }
        }

        if (!episode.productionCode.isNullOrEmpty()) {
            Text(
                text = stringResource(Res.string.label_production_code, episode.productionCode),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        CrewSummary(crew = episode.allCrew, onPersonClicked = onPersonClicked)

        episode.externalIds?.imdbId?.let { imdbId ->
            Text(
                text = stringResource(Res.string.action_view_on_imdb),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .clickable { openUrl("https://www.imdb.com/title/$imdbId/") },
            )
        }
    }
}

// One line per role group, e.g. "Directed by Timothy Van Patten" / "Written by D.B. Weiss, David
// Benioff". Departments beyond directing/writing (camera, editing, ...) are collapsed into a
// single crew count line to keep the header scannable.
@Composable
private fun CrewSummary(
    crew: List<CrewMember>,
    onPersonClicked: (Long) -> Unit,
) {
    if (crew.isEmpty()) return
    val directors = crew.filter { it.job == "Director" }
    val writers = crew.filter { it.department == "Writing" }.map { it.name }.distinct()
    val others = crew.size - crew.count { it.job == "Director" || it.department == "Writing" }
    Column(modifier = Modifier.padding(top = 6.dp)) {
        DirectedByLine(directors = directors, onPersonClicked = onPersonClicked)
        if (writers.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.label_written_by, writers.joinToString(", ")),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (others > 0) {
            Text(
                text = "$others more crew member${if (others == 1) "" else "s"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun formatEpisodeType(type: String): String =
    type.split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
