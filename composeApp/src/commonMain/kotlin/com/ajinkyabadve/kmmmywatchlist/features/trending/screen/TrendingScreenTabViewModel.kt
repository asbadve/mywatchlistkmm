package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.constant.FeatureFlags
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.TIME_WINDOW_DAY
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.TIME_WINDOW_WEEK
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.trendingChipList
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.latestTrailerVideo
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrendingRepository
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrendingRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_trailers

@OptIn(ExperimentalSerializationApi::class)
class TrendingScreenTabViewModel(
    private val trendingRepository: TrendingRepository = TrendingRepositoryImpl(),
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
    private val tvRepository: TvRepository = TvRepositoryImpl(),
    // Parked behind a flag: the Latest Trailers rail fans out into ~11 requests (a source list plus
    // one videos call per title) on load, which starves the rest of the tab on slow networks. See
    // [FeatureFlags.TRENDING_TRAILERS_ENABLED]. Off => skip the init fetch entirely.
    private val trailersEnabled: Boolean = FeatureFlags.TRENDING_TRAILERS_ENABLED,
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _isScreenLoading = MutableStateFlow(false)
    val isScreenLoading = _isScreenLoading

    private val _isMovieTrendScreenLoading = MutableStateFlow(false)
    val isMovieTrendScreenLoading = _isMovieTrendScreenLoading

    private val _isMovieTrendLoading = MutableStateFlow(false)
    val isMovieTrendLoading = _isMovieTrendLoading

    private val _trendMovieList = MutableStateFlow<List<Movie>>(listOf())
    val trendMovieList = _trendMovieList

    private val _trendMovieChipList = MutableStateFlow(trendingChipList)
    val trendMovieChipList = _trendMovieChipList

    private val _selectedMovieChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedMovieChipIndex = _selectedMovieChipIndex

    private val _selectedTvChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedTvChipIndex = _selectedTvChipIndex

    private val _trendTvList = MutableStateFlow<List<Movie>>(listOf())
    val trendTvList = _trendTvList

    private val _trendTvChipList = MutableStateFlow(trendingChipList)
    val trendTvChipList = _trendTvChipList

    private val _isTvTrendScreenLoading = MutableStateFlow(false)
    val isTvTrendScreenLoading = _isTvTrendScreenLoading

    private val _isTvTrendLoading = MutableStateFlow(false)
    val isTvTrendLoading = _isTvTrendLoading

    private val _selectedPeopleChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedPeopleChipIndex = _selectedPeopleChipIndex

    private val _trendPeopleList = MutableStateFlow<List<Movie>>(listOf())
    val trendPeopleList = _trendPeopleList

    private val _trendPeopleChipList = MutableStateFlow(trendingChipList)
    val trendPeopleChipList = _trendPeopleChipList

    private val _isPeopleTrendScreenLoading = MutableStateFlow(false)
    val isPeopleTrendScreenLoading = _isPeopleTrendScreenLoading

    private val _isPeopleTrendLoading = MutableStateFlow(false)
    val isPeopleTrendLoading = _isPeopleTrendLoading

    private val _movieTrendError = MutableStateFlow<String?>(null)
    val movieTrendError = _movieTrendError

    private val _tvTrendError = MutableStateFlow<String?>(null)
    val tvTrendError = _tvTrendError

    private val _peopleTrendError = MutableStateFlow<String?>(null)
    val peopleTrendError = _peopleTrendError

    private val _selectedTrailerSource = MutableStateFlow(TrailerSource.IN_THEATERS)
    val selectedTrailerSource = _selectedTrailerSource

    private val _trailerList = MutableStateFlow<List<Trailer>>(emptyList())
    val trailerList = _trailerList

    private val _isTrailerScreenLoading = MutableStateFlow(false)
    val isTrailerScreenLoading = _isTrailerScreenLoading

    private val _isTrailerLoading = MutableStateFlow(false)
    val isTrailerLoading = _isTrailerLoading

    private val _trailerError = MutableStateFlow<UiText?>(null)
    val trailerError = _trailerError

    // Trailers per source are stable within a session; cache so chip switches don't refetch.
    private val trailerCache = mutableMapOf<TrailerSource, List<Trailer>>()

    init {
        _isScreenLoading.value = true
        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MediaTypeConstant.MOVIE,
            true,
        )
        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MediaTypeConstant.TV,
            true,
        )

        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MediaTypeConstant.PERSON,
            true,
        )

        if (trailersEnabled) {
            loadTrailers(TrailerSource.IN_THEATERS, isFirstLoad = true)
        }
    }

    private fun setErrorStateByMediaType(
        mediaType: String,
        message: String?,
    ) {
        when (mediaType) {
            MediaTypeConstant.MOVIE -> _movieTrendError.value = message
            MediaTypeConstant.TV -> _tvTrendError.value = message
            MediaTypeConstant.PERSON -> _peopleTrendError.value = message
        }
    }

    private fun loadTrendingMedia(
        timeWindow: String,
        mediaType: String,
        isFirstLoad: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            setErrorStateByMediaType(mediaType, null)
            if (isFirstLoad) {
                setScreenLoadingStateByMediaType(mediaType = mediaType, isLoading = true)
            } else {
                setLoadingStateByMediaType(mediaType = mediaType, isLoading = true)
            }
            try {
                val movies =
                    trendingRepository
                        .getTrending(
                            timeWindow,
                            mediaType,
                        ).list
                _isScreenLoading.value = false
                movies?.let {
                    setErrorStateByMediaType(mediaType, null)
                    when (mediaType) {
                        MediaTypeConstant.MOVIE -> {
                            _trendMovieList.value = movies
                        }

                        MediaTypeConstant.TV -> {
                            _trendTvList.value = movies
                        }

                        MediaTypeConstant.PERSON -> {
                            _trendPeopleList.value = movies
                        }

                        else -> {
                        }
                    }
                } ?: run {
                    setErrorStateByMediaType(mediaType, "No content found.")
                }
            } catch (e: HttpExceptions) {
                setErrorStateByMediaType(mediaType, e.message)
                Napier.d { "HTTP exception: " + e.message }
            } catch (e: IOException) {
                setErrorStateByMediaType(mediaType, "Network error. Please check your connection.")
                Napier.d { "Network IO exception: " + e.message }
            } catch (e: SerializationException) {
                setErrorStateByMediaType(mediaType, "Failed to parse content.")
                Napier.d { "Serialization exception: " + e.message }
            } finally {
                _isScreenLoading.value = false
                if (isFirstLoad) {
                    setScreenLoadingStateByMediaType(mediaType = mediaType, isLoading = false)
                } else {
                    setLoadingStateByMediaType(mediaType = mediaType, isLoading = false)
                }
            }
        }
    }

    private fun setScreenLoadingStateByMediaType(
        mediaType: String,
        isLoading: Boolean,
    ) {
        when (mediaType) {
            MediaTypeConstant.MOVIE -> {
                _isMovieTrendScreenLoading.value = isLoading
            }

            MediaTypeConstant.TV -> {
                _isTvTrendScreenLoading.value = isLoading
            }

            MediaTypeConstant.PERSON -> {
                _isPeopleTrendScreenLoading.value = isLoading
            }

            else -> {
            }
        }
    }

    private fun setLoadingStateByMediaType(
        mediaType: String,
        isLoading: Boolean,
    ) {
        when (mediaType) {
            MediaTypeConstant.MOVIE -> {
                _isMovieTrendLoading.value = isLoading
            }

            MediaTypeConstant.TV -> {
                _isTvTrendLoading.value = isLoading
            }

            MediaTypeConstant.PERSON -> {
                _isPeopleTrendLoading.value = isLoading
            }

            else -> {
            }
        }
    }

    fun onChipSelected(
        selectedIndex: Int,
        mediaType: String,
    ) {
        when (mediaType) {
            MediaTypeConstant.MOVIE -> {
                _selectedMovieChipIndex.value = selectedIndex
            }

            MediaTypeConstant.TV -> {
                _selectedTvChipIndex.value = selectedIndex
            }

            MediaTypeConstant.PERSON -> {
                _selectedPeopleChipIndex.value = selectedIndex
            }

            else -> {
            }
        }

        loadTrendingMedia(
            getSelectedTimeWindow(selectedIndex),
            mediaType,
            false,
        )
    }

    private fun getSelectedTimeWindow(selectedIndex: Int): String =
        when (selectedIndex) {
            0 -> {
                TIME_WINDOW_DAY
            }

            1 -> {
                TIME_WINDOW_WEEK
            }

            else -> {
                TIME_WINDOW_DAY
            }
        }

    fun onTrailerSourceSelected(source: TrailerSource) {
        _selectedTrailerSource.value = source
        loadTrailers(source, isFirstLoad = false)
    }

    private fun loadTrailers(
        source: TrailerSource,
        isFirstLoad: Boolean,
    ) {
        trailerCache[source]?.let {
            _trailerList.value = it
            _trailerError.value = null
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            _trailerError.value = null
            if (isFirstLoad) {
                _isTrailerScreenLoading.value = true
            } else {
                _isTrailerLoading.value = true
            }
            try {
                val trailers = fetchTrailers(source)
                trailerCache[source] = trailers
                if (_selectedTrailerSource.value == source) {
                    _trailerList.value = trailers
                }
            } catch (e: HttpExceptions) {
                Napier.d { "HTTP exception fetching trailers: " + e.message }
                _trailerError.value = UiText.Plain(e.message)
            } catch (e: IOException) {
                Napier.d { "Network IO exception fetching trailers: " + e.message }
                _trailerError.value = UiText.Resource(Res.string.error_network)
            } catch (e: ContentConvertException) {
                Napier.d { "Malformed response fetching trailers: " + e.message }
                _trailerError.value = UiText.Resource(Res.string.error_unexpected_trailers)
            } catch (e: SerializationException) {
                Napier.d { "Serialization exception fetching trailers: " + e.message }
                _trailerError.value = UiText.Resource(Res.string.error_unexpected_trailers)
            } finally {
                _isTrailerScreenLoading.value = false
                _isTrailerLoading.value = false
            }
        }
    }

    // List endpoints can't append videos, so mirror the TMDB homepage: take the first page of the
    // source list, fetch each title's videos in parallel, keep the best trailer per title, newest
    // first. A single title's failed videos call just drops that title from the rail.
    private suspend fun fetchTrailers(source: TrailerSource): List<Trailer> {
        val candidates: List<TrailerCandidate> =
            when (source) {
                TrailerSource.IN_THEATERS -> movieCandidates(MoviesConstant.NOW_PLAYING_API_PATH)
                TrailerSource.UPCOMING -> movieCandidates(MoviesConstant.UPCOMING_API_PATH)
                TrailerSource.POPULAR -> movieCandidates(MoviesConstant.POPULAR_API_PATH)
                TrailerSource.ON_TV -> tvCandidates(TvShowsConstant.ON_THE_AIR_API_PATH)
            }
        return coroutineScope {
            candidates
                .take(MAX_TITLES_PER_SOURCE)
                .map { candidate ->
                    async {
                        val videos = fetchVideosOrNull(candidate) ?: return@async null
                        latestTrailerVideo(videos)?.let { video ->
                            Trailer(
                                mediaId = candidate.mediaId,
                                isMovie = candidate.isMovie,
                                mediaTitle = candidate.title,
                                backdropPath = candidate.backdropPath,
                                video = video,
                            )
                        }
                    }
                }.awaitAll()
        }.filterNotNull().sortedByDescending { it.video.publishedAt }
    }

    private suspend fun movieCandidates(fetchType: String): List<TrailerCandidate> =
        movieRepository.getMovies(FIRST_PAGE, fetchType).list.orEmpty().map { movie ->
            TrailerCandidate(
                mediaId = movie.id.toLong(),
                isMovie = true,
                title = movie.title,
                backdropPath = movie.backdropPath,
            )
        }

    private suspend fun tvCandidates(fetchType: String): List<TrailerCandidate> =
        tvRepository.getTvShows(FIRST_PAGE, fetchType).list.orEmpty().map { show ->
            TrailerCandidate(
                mediaId = show.id.toLong(),
                isMovie = false,
                title = show.title,
                backdropPath = show.backdropPath,
            )
        }

    private suspend fun fetchVideosOrNull(candidate: TrailerCandidate) =
        try {
            if (candidate.isMovie) {
                movieRepository.getMovieVideos(candidate.mediaId).results
            } else {
                tvRepository.getTvVideos(candidate.mediaId).results
            }
        } catch (e: HttpExceptions) {
            logVideosFailure(candidate, e)
            null
        } catch (e: IOException) {
            logVideosFailure(candidate, e)
            null
        } catch (e: ContentConvertException) {
            logVideosFailure(candidate, e)
            null
        } catch (e: SerializationException) {
            logVideosFailure(candidate, e)
            null
        }

    private fun logVideosFailure(
        candidate: TrailerCandidate,
        throwable: Throwable,
    ) {
        val mediaType = if (candidate.isMovie) MediaTypeConstant.MOVIE else MediaTypeConstant.TV
        Napier.d { "Failed to fetch videos for $mediaType ${candidate.mediaId}: ${throwable.message}" }
    }

    private data class TrailerCandidate(
        val mediaId: Long,
        val isMovie: Boolean,
        val title: String,
        val backdropPath: String?,
    )

    companion object {
        const val DEFAULT_SELECTED_CHIP = 0
        private const val FIRST_PAGE = 1
        private const val MAX_TITLES_PER_SOURCE = 10
    }
}
