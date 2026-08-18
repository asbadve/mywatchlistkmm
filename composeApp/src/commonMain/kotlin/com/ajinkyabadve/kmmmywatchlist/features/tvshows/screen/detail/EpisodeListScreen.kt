package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.episode_latest_badge
import mywatchlist.composeapp.generated.resources.no_episodes_available
import mywatchlist.composeapp.generated.resources.no_overview_available_episode
import mywatchlist.composeapp.generated.resources.title_episodes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EpisodeListScreen(
    tvShowId: Long,
    seasonNumber: Int,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Int) -> Unit,
    viewModel: EpisodeListScreenModel =
        viewModel(key = "EpisodeListScreenModel:$tvShowId:$seasonNumber") { EpisodeListScreenModel(tvShowId, seasonNumber) },
) {
    val uiState by viewModel.uiState.collectAsState()

    // This screen is the detail pane of the Seasons<->Episodes list-detail scaffold. When the
    // season list is visible alongside it (wide layout), it already offers its own way back, so
    // this screen's back arrow would be redundant - only show it when this is the only pane visible.
    val listDetailScope = LocalListDetailSceneScope.current
    val showBackButton =
        listDetailScope?.let {
            it.scaffoldTransitionScope.scaffoldStateTransition.currentState.secondary == PaneAdaptedValue.Hidden
        } ?: true

    val listState = rememberLazyListState()

    // Hide-on-scroll-down / reveal-on-scroll-up, matching the app's collapsing bottom nav.
    //
    // `canScroll` is not optional: left at its default of `{ true }`, Material3 moves the bar for
    // any drag, so a screen whose content fits would lose its bar - and its only back affordance -
    // without ever having scrolled. See CollapsibleBarState for the same fix on the bars this app
    // drives itself.
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            canScroll = { listState.canScrollForward || listState.canScrollBackward },
        )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                title = (uiState as? EpisodeListState.Success)?.season?.name ?: stringResource(Res.string.title_episodes),
                onBackClicked = onBackClicked,
                scrollBehavior = scrollBehavior,
                showBackButton = showBackButton,
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
                is EpisodeListState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EpisodeListState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadEpisodes() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is EpisodeListState.Success -> {
                    if (state.season.episodes.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.no_episodes_available),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    } else {
                        val sortedEpisodes = remember(state.season) { state.season.episodes.sortedBy { it.episodeNumber } }
                        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
                        // The latest episode that has actually aired, verified against today's date
                        // rather than assuming the highest episode number has released - a season
                        // can list future episodes ahead of time. Ties on air date (e.g. a whole
                        // season dropping on one day) break toward the higher episode number.
                        val latestReleasedEpisodeNumber =
                            remember(sortedEpisodes, today) {
                                sortedEpisodes
                                    .filter { it.isReleased(today) }
                                    .maxWithOrNull(compareBy({ it.airDate.orEmpty() }, { it.episodeNumber }))
                                    ?.episodeNumber
                            }

                        LaunchedEffect(latestReleasedEpisodeNumber) {
                            val index = sortedEpisodes.indexOfFirst { it.episodeNumber == latestReleasedEpisodeNumber }
                            if (index >= 0) listState.scrollToItem(index)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            items(sortedEpisodes) { episode ->
                                EpisodeListItem(
                                    episode = episode,
                                    isLatestReleased = episode.episodeNumber == latestReleasedEpisodeNumber,
                                    onClick = { onEpisodeClicked(episode.episodeNumber) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: Episode,
    onClick: () -> Unit,
    isLatestReleased: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (isLatestReleased) {
                        Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
    ) {
        val density = LocalDensity.current.density
        val stillUrl =
            ImageConfigResolver.resolve(
                path = episode.stillPath,
                type = ImageConfigResolver.ImageType.STILL,
                targetWidthDp = 150,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .width(140.dp)
                    .aspectRatio(16 / 9f)
                    .clip(RoundedCornerShape(8.dp))
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isLatestReleased) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.episode_latest_badge),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (!episode.airDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.airDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = episode.overview.ifEmpty { stringResource(Res.string.no_overview_available_episode) },
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }
    }
}
