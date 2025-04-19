@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class,
)

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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_MOVIE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_PEOPLE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_TV
import com.ajinkyabadve.kmmmywatchlist.homepage.model.AppTabs.TRENDING
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import io.github.aakira.napier.Napier
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class TrendingScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel =
            rememberScreenModel(
                tag = TRENDING,
                factory = {
                    TrendingScreenModel()
                },
            )

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

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
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

                setupTrendingMovies(
                    movieChipList,
                    movieChipSelected,
                    viewModel,
                    movieTrendScreenLoadingState,
                    movieTrendLoadingState,
                    movieTrendResult,
                )

                setupTrendingTvShows(
                    tvChipList,
                    tvChipSelected,
                    viewModel,
                    tvTrendScreenLoadingState,
                    tvTrendLoadingState,
                    tvTrendResult,
                )

                setupTrendingPeople(
                    peopleChipList,
                    peopleChipSelected,
                    viewModel,
                    peopleTrendScreenLoadingState,
                    peopleTrendLoadingState,
                    peopleTrendResult,
                )
            }
        }
    }

    @Composable
    private fun TrendingScreen.setupTrendingPeople(
        peopleChipList: List<String>,
        peopleChipSelected: Int,
        viewModel: TrendingScreenModel,
        peopleTrendScreenLoadingState: Boolean,
        peopleTrendLoadingState: Boolean,
        peopleTrendResult: List<Movie>,
    ) {
        addPeopleChips(peopleChipList, peopleChipSelected, viewModel)

        if (peopleTrendScreenLoadingState) {
            trendingLoadingState()
        }

        if (peopleTrendLoadingState) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (peopleTrendResult.isNotEmpty()) {
            trendingMediaCarousel(peopleTrendResult)
        }
    }

    @Composable
    private fun TrendingScreen.setupTrendingTvShows(
        tvChipList: List<String>,
        tvChipSelected: Int,
        viewModel: TrendingScreenModel,
        tvTrendScreenLoadingState: Boolean,
        tvTrendLoadingState: Boolean,
        tvTrendResult: List<Movie>,
    ) {
        addTvChips(tvChipList, tvChipSelected, viewModel)

        if (tvTrendScreenLoadingState) {
            trendingLoadingState()
        }

        if (tvTrendLoadingState) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (tvTrendResult.isNotEmpty()) {
            trendingMediaCarousel(tvTrendResult)
        }
    }

    @Composable
    private fun TrendingScreen.setupTrendingMovies(
        movieChipList: List<String>,
        movieChipSelected: Int,
        viewModel: TrendingScreenModel,
        movieTrendScreenLoadingState: Boolean,
        movieTrendLoadingState: Boolean,
        movieTrendResult: List<Movie>,
    ) {
        addMovieChips(movieChipList, movieChipSelected, viewModel)

        if (movieTrendScreenLoadingState) {
            trendingLoadingState()
        }

        if (movieTrendLoadingState) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (movieTrendResult.isNotEmpty()) {
            trendingMediaCarousel(movieTrendResult)
        }
    }

    @Composable
    private fun TrendingScreen.addTvChips(
        tvChipList: List<String>,
        tvChipSelected: Int,
        viewModel: TrendingScreenModel,
    ) {
        if (tvChipList.isNotEmpty()) {
            addMediaChips(
                tvChipSelected,
                tvChipList,
                viewModel,
                "Trending Tv show",
                MEDIA_TYPE_TV,
            )
        }
    }

    @Composable
    private fun TrendingScreen.addPeopleChips(
        peopleChipList: List<String>,
        peopleChipSelected: Int,
        viewModel: TrendingScreenModel,
    ) {
        if (peopleChipList.isNotEmpty()) {
            addMediaChips(
                peopleChipSelected,
                peopleChipList,
                viewModel,
                "Trending People",
                MEDIA_TYPE_PEOPLE,
            )
        }
    }

    @Composable
    private fun TrendingScreen.addMovieChips(
        movieChipList: List<String>,
        movieChipSelected: Int,
        viewModel: TrendingScreenModel,
    ) {
        if (movieChipList.isNotEmpty()) {
            addMediaChips(
                movieChipSelected,
                movieChipList,
                viewModel,
                "Trending Movies",
                MEDIA_TYPE_MOVIE,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun trendingMediaCarousel(mediaTrendResult: List<Movie>) {
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
                MoviesTab.Tabs.IMAGE_BASE_URL + item.posterPath,
                item.title,
                modifier = Modifier,
                onClick = {
                    Napier.d { "title" + item.title }
                },
            )
        }
    }

    @Composable
    private fun addMediaChips(
        movieChipSelected: Int,
        movieChipList: List<String>,
        viewModel: TrendingScreenModel,
        chipsHeading: String,
        mediaType: String,
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
                text = chipsHeading,
                style = MaterialTheme.typography.titleMedium,
            )

            scrollableChips(
                selectedChip = movieChipSelected,
                chipItemList = movieChipList,
                onClick = { selectedIndex ->
                    viewModel.onChipSelected(selectedIndex, mediaType)
                },
            )
        }
    }
}

@Composable
private fun trendingLoadingState() {
    val items = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier.fillMaxWidth(),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        mediaMovieRow(
            null,
            "loading",
            modifier = Modifier,
            onClick = {
            },
            isLoadingState = true,
        )
    }
}
