@file:Suppress("ktlint:standard:function-naming")

package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.constant.FeatureFlags
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.mediaPersonRow
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrendingSectionState
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.mediaTvShowRow
import io.github.aakira.napier.Napier
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun TrendingScreenTab(
    modifier: Modifier = Modifier,
    viewModel: TrendingScreenTabViewModel = viewModel { TrendingScreenTabViewModel() },
    onMovieSelected: (Long) -> Unit = {},
    onTvShowSelected: (Long) -> Unit = {},
    onPersonSelected: (Long) -> Unit = {},
) {
    val screenLoadingState by viewModel.isScreenLoading.collectAsState()
    val movieTrendScreenLoadingState by viewModel.isMovieTrendScreenLoading.collectAsState()
    val movieTrendLoadingState by viewModel.isMovieTrendLoading.collectAsState()
    val movieTrendResult by viewModel.trendMovieList.collectAsState()
    val movieChipList by viewModel.trendMovieChipList.collectAsState()
    val movieChipSelected by viewModel.selectedMovieChipIndex.collectAsState()
    val movieTrendError by viewModel.movieTrendError.collectAsState()
    val tvTrendScreenLoadingState by viewModel.isTvTrendScreenLoading.collectAsState()
    val tvTrendLoadingState by viewModel.isTvTrendLoading.collectAsState()
    val tvTrendResult by viewModel.trendTvList.collectAsState()
    val tvChipList by viewModel.trendTvChipList.collectAsState()
    val tvChipSelected by viewModel.selectedTvChipIndex.collectAsState()
    val tvTrendError by viewModel.tvTrendError.collectAsState()
    val peopleTrendScreenLoadingState by viewModel.isPeopleTrendScreenLoading.collectAsState()
    val peopleTrendLoadingState by viewModel.isPeopleTrendLoading.collectAsState()
    val peopleTrendResult by viewModel.trendPeopleList.collectAsState()
    val peopleChipList by viewModel.trendPeopleChipList.collectAsState()
    val peopleChipSelected by viewModel.selectedPeopleChipIndex.collectAsState()
    val peopleTrendError by viewModel.peopleTrendError.collectAsState()

    val sections =
        listOf(
            TrendingSectionState(
                title = "Trending Movies",
                mediaType = MediaTypeConstant.MOVIE,
                chipList = movieChipList,
                selectedChipIndex = movieChipSelected,
                isScreenLoading = movieTrendScreenLoadingState,
                isLoading = movieTrendLoadingState,
                mediaList = movieTrendResult,
                errorMessage = movieTrendError,
            ),
            TrendingSectionState(
                title = "Trending Tv show",
                mediaType = MediaTypeConstant.TV,
                chipList = tvChipList,
                selectedChipIndex = tvChipSelected,
                isScreenLoading = tvTrendScreenLoadingState,
                isLoading = tvTrendLoadingState,
                mediaList = tvTrendResult,
                errorMessage = tvTrendError,
            ),
            TrendingSectionState(
                title = "Trending People",
                mediaType = MediaTypeConstant.PERSON,
                chipList = peopleChipList,
                selectedChipIndex = peopleChipSelected,
                isScreenLoading = peopleTrendScreenLoadingState,
                isLoading = peopleTrendLoadingState,
                mediaList = peopleTrendResult,
                errorMessage = peopleTrendError,
            ),
        )

    TrendingScreenContent(
        modifier = modifier,
        screenLoadingState = screenLoadingState,
        sections = sections,
        onChipSelected = viewModel::onChipSelected,
        onMovieSelected = onMovieSelected,
        onTvShowSelected = onTvShowSelected,
        onPersonSelected = onPersonSelected,
        trailersViewModel = viewModel,
    )
}

@Composable
fun TrendingScreenContent(
    screenLoadingState: Boolean,
    sections: List<TrendingSectionState>,
    onChipSelected: (Int, String) -> Unit,
    onMovieSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onTvShowSelected: (Long) -> Unit = {},
    onPersonSelected: (Long) -> Unit = {},
    trailersViewModel: TrendingScreenTabViewModel? = null,
    // Parked behind a flag, in lockstep with the ViewModel's init fetch. See
    // [FeatureFlags.TRENDING_TRAILERS_ENABLED].
    showTrailers: Boolean = FeatureFlags.TRENDING_TRAILERS_ENABLED,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
        ) {
            if (screenLoadingState) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (showTrailers) {
                trailersViewModel?.let { LatestTrailersSection(viewModel = it) }
            }

            sections.forEach { section ->
                TrendingSection(
                    section = section,
                    onChipSelected = onChipSelected,
                    onMovieSelected = onMovieSelected,
                    onTvShowSelected = onTvShowSelected,
                    onPersonSelected = onPersonSelected,
                )
            }
        }
    }
}

@Composable
private fun TrendingSection(
    section: TrendingSectionState,
    onChipSelected: (Int, String) -> Unit,
    onMovieSelected: (Long) -> Unit,
    onTvShowSelected: (Long) -> Unit,
    onPersonSelected: (Long) -> Unit,
) {
    if (section.chipList.isNotEmpty()) {
        MediaChips(
            section = section,
            onChipSelected = onChipSelected,
        )
    }

    if (section.isScreenLoading) {
        TrendingLoadingState()
    }

    if (section.isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    if (section.errorMessage != null) {
        TrendingErrorState(
            errorMessage = section.errorMessage,
            onRetry = {
                onChipSelected(section.selectedChipIndex, section.mediaType)
            },
        )
    } else if (section.mediaList.isNotEmpty()) {
        TrendingMediaCarousel(section.mediaList, section.mediaType, onMovieSelected, onTvShowSelected, onPersonSelected)
    }
}

@Composable
private fun MediaChips(
    section: TrendingSectionState,
    onChipSelected: (Int, String) -> Unit,
) {
    Row(
        modifier =
            Modifier.padding(
                start = 24.dp,
                top = 0.dp,
                bottom = 0.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
        )

        scrollableChips(
            selectedChip = section.selectedChipIndex,
            chipItemList = section.chipList,
            onClick = { selectedIndex ->
                onChipSelected(selectedIndex, section.mediaType)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingMediaCarousel(
    mediaTrendResult: List<Movie>,
    mediaType: String,
    onMovieSelected: (Long) -> Unit,
    onTvShowSelected: (Long) -> Unit,
    onPersonSelected: (Long) -> Unit,
) {
    val state = rememberCarouselState { mediaTrendResult.count() }
    HorizontalMultiBrowseCarousel(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        preferredItemWidth = 200.dp,
        flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = state),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { i ->
        val item = mediaTrendResult[i]
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val targetWidth = 200
        val imageType =
            if (mediaType == MediaTypeConstant.PERSON) {
                com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.PROFILE
            } else {
                com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER
            }
        val imageUrl =
            com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.resolve(
                path = item.posterPath,
                type = imageType,
                targetWidthDp = targetWidth,
                density = density,
            )
        when (mediaType) {
            MediaTypeConstant.TV ->
                mediaTvShowRow(
                    imageUrl = imageUrl,
                    title = item.title,
                    modifier = Modifier,
                    onClick = {
                        Napier.d { "title " + item.title }
                        onTvShowSelected(item.id.toLong())
                    },
                )
            MediaTypeConstant.PERSON ->
                mediaPersonRow(
                    imageUrl = imageUrl,
                    name = item.title,
                    modifier = Modifier,
                    onClick = {
                        Napier.d { "title " + item.title }
                        onPersonSelected(item.id.toLong())
                    },
                )
            else ->
                mediaMovieRow(
                    imageUrl = imageUrl,
                    title = item.title,
                    modifier = Modifier,
                    onClick = {
                        Napier.d { "title " + item.title }
                        onMovieSelected(item.id.toLong())
                    },
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingLoadingState() {
    val items = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier.fillMaxWidth(),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        mediaMovieRow(
            imageUrl = null,
            title = "loading",
            modifier = Modifier,
            onClick = {},
            isLoadingState = true,
        )
    }
}

@Composable
private fun TrendingErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Couldn't Load Content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onRetry,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Retry")
                }
            }
        }
    }
}
