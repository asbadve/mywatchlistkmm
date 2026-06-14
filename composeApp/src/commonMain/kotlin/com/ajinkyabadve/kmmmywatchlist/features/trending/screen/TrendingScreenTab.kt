@file:Suppress("ktlint:standard:function-naming")

package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_MOVIE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_PEOPLE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_TV
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrendingSectionState
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import io.github.aakira.napier.Napier
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun TrendingScreenTab(
    modifier: Modifier = Modifier,
    viewModel: TrendingScreenTabViewModel = viewModel { TrendingScreenTabViewModel() },
) {
    val screenLoadingState by viewModel.isScreenLoading.collectAsState()
    val movieTrendScreenLoadingState by viewModel.isMovieTrendScreenLoading.collectAsState()
    val movieTrendLoadingState by viewModel.isMovieTrendLoading.collectAsState()
    val movieTrendResult by viewModel.trendMovieList.collectAsState()
    val movieChipList by viewModel.trendMovieChipList.collectAsState()
    val movieChipSelected by viewModel.selectedMovieChipIndex.collectAsState()
    val tvTrendScreenLoadingState by viewModel.isTvTrendScreenLoading.collectAsState()
    val tvTrendLoadingState by viewModel.isTvTrendLoading.collectAsState()
    val tvTrendResult by viewModel.trendTvList.collectAsState()
    val tvChipList by viewModel.trendTvChipList.collectAsState()
    val tvChipSelected by viewModel.selectedTvChipIndex.collectAsState()
    val peopleTrendScreenLoadingState by viewModel.isPeopleTrendScreenLoading.collectAsState()
    val peopleTrendLoadingState by viewModel.isPeopleTrendLoading.collectAsState()
    val peopleTrendResult by viewModel.trendPeopleList.collectAsState()
    val peopleChipList by viewModel.trendPeopleChipList.collectAsState()
    val peopleChipSelected by viewModel.selectedPeopleChipIndex.collectAsState()

    val sections =
        listOf(
            TrendingSectionState(
                title = "Trending Movies",
                mediaType = MEDIA_TYPE_MOVIE,
                chipList = movieChipList,
                selectedChipIndex = movieChipSelected,
                isScreenLoading = movieTrendScreenLoadingState,
                isLoading = movieTrendLoadingState,
                mediaList = movieTrendResult,
            ),
            TrendingSectionState(
                title = "Trending Tv show",
                mediaType = MEDIA_TYPE_TV,
                chipList = tvChipList,
                selectedChipIndex = tvChipSelected,
                isScreenLoading = tvTrendScreenLoadingState,
                isLoading = tvTrendLoadingState,
                mediaList = tvTrendResult,
            ),
            TrendingSectionState(
                title = "Trending People",
                mediaType = MEDIA_TYPE_PEOPLE,
                chipList = peopleChipList,
                selectedChipIndex = peopleChipSelected,
                isScreenLoading = peopleTrendScreenLoadingState,
                isLoading = peopleTrendLoadingState,
                mediaList = peopleTrendResult,
            ),
        )

    TrendingScreenContent(
        modifier = modifier,
        screenLoadingState = screenLoadingState,
        sections = sections,
        onChipSelected = viewModel::onChipSelected,
    )
}

@Composable
fun TrendingScreenContent(
    screenLoadingState: Boolean,
    sections: List<TrendingSectionState>,
    onChipSelected: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
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

            sections.forEach { section ->
                TrendingSection(
                    section = section,
                    onChipSelected = onChipSelected,
                )
            }
        }
    }
}

@Composable
private fun TrendingSection(
    section: TrendingSectionState,
    onChipSelected: (Int, String) -> Unit,
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

    if (section.mediaList.isNotEmpty()) {
        TrendingMediaCarousel(section.mediaList)
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
private fun TrendingMediaCarousel(mediaTrendResult: List<Movie>) {
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
        mediaMovieRow(
            imageUrl = MoviesTab.Tabs.IMAGE_BASE_URL + item.posterPath,
            title = item.title,
            modifier = Modifier,
            onClick = {
                Napier.d { "title" + item.title }
            },
        )
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
