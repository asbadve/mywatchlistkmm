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
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrailerCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrailerCacheRepositoryImpl
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    // See [FeatureFlags.TRENDING_TRAILERS_ENABLED]. Off => skip the init fetch entirely.
    private val trailersEnabled: Boolean = FeatureFlags.TRENDING_TRAILERS_ENABLED,
    private val trailerCacheRepository: TrailerCacheRepository = TrailerCacheRepositoryImpl(),
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

    // In-memory layer over [trailerCacheRepository], so chip switches within a session never even
    // decode JSON again.
    private val trailerCache = mutableMapOf<TrailerSource, List<Trailer>>()

    // Sources being fetched right now, with the cards found so far - so re-selecting a chip while
    // its fetch is still running shows the partial rail instead of starting a second fetch.
    private val trailersInProgress = mutableMapOf<TrailerSource, List<Trailer>>()

    init {
        _isScreenLoading.value = true
        val trendingLoads =
            listOf(MediaTypeConstant.MOVIE, MediaTypeConstant.TV, MediaTypeConstant.PERSON).map { mediaType ->
                loadTrendingMedia(getSelectedTimeWindow(DEFAULT_SELECTED_CHIP), mediaType, true)
            }

        if (trailersEnabled) {
            // The trailer rail's ~11 requests wait for the trending rows' first load, so they never
            // compete with what the tab is mainly for. A cached rail still shows immediately.
            loadTrailers(TrailerSource.IN_THEATERS, isFirstLoad = true, after = trendingLoads)
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
    ): Job =
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
        after: List<Job> = emptyList(),
    ) {
        val cached = trailerCache[source] ?: trailerCacheRepository.get(source)?.also { trailerCache[source] = it }
        if (cached != null) {
            _trailerList.value = cached
            _trailerError.value = null
            setTrailerLoading(isFirstLoad, loading = false)
            return
        }
        trailersInProgress[source]?.let { partial ->
            _trailerList.value = partial
            _trailerError.value = null
            setTrailerLoading(isFirstLoad, loading = true)
            return
        }
        trailersInProgress[source] = emptyList()
        _trailerList.value = emptyList()
        _trailerError.value = null
        setTrailerLoading(isFirstLoad, loading = true)
        viewModelScope.launch(Dispatchers.Main) {
            try {
                after.joinAll()
                val trailers =
                    fetchTrailers(source) { partial ->
                        trailersInProgress[source] = partial
                        if (_selectedTrailerSource.value == source) _trailerList.value = partial
                    }
                trailerCache[source] = trailers
                trailerCacheRepository.put(source, trailers)
                if (_selectedTrailerSource.value == source) {
                    _trailerList.value = trailers
                }
            } catch (e: HttpExceptions) {
                Napier.d { "HTTP exception fetching trailers: " + e.message }
                showTrailerError(source, UiText.Plain(e.message))
            } catch (e: IOException) {
                Napier.d { "Network IO exception fetching trailers: " + e.message }
                showTrailerError(source, UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                Napier.d { "Malformed response fetching trailers: " + e.message }
                showTrailerError(source, UiText.Resource(Res.string.error_unexpected_trailers))
            } catch (e: SerializationException) {
                Napier.d { "Serialization exception fetching trailers: " + e.message }
                showTrailerError(source, UiText.Resource(Res.string.error_unexpected_trailers))
            } finally {
                trailersInProgress.remove(source)
                if (_selectedTrailerSource.value == source) setTrailerLoading(isFirstLoad, loading = false)
            }
        }
    }

    private fun setTrailerLoading(
        isFirstLoad: Boolean,
        loading: Boolean,
    ) {
        _isTrailerScreenLoading.value = loading && isFirstLoad
        _isTrailerLoading.value = loading && !isFirstLoad
    }

    private fun showTrailerError(
        source: TrailerSource,
        error: UiText,
    ) {
        if (_selectedTrailerSource.value == source) _trailerError.value = error
    }

    /**
     * Mirrors the TMDB homepage, since list endpoints can't append videos: the first page of the
     * source list, then each title's videos, keeping its newest trailer. At most
     * [MAX_CONCURRENT_VIDEO_REQUESTS] videos calls run at once - the same total, but no burst on a
     * slow connection - and [onProgress] gets the rail so far after each title, so cards appear as
     * they arrive instead of after the slowest call. A title whose videos call fails is dropped.
     */
    private suspend fun fetchTrailers(
        source: TrailerSource,
        onProgress: (List<Trailer>) -> Unit,
    ): List<Trailer> {
        val candidates: List<TrailerCandidate> =
            when (source) {
                TrailerSource.IN_THEATERS -> movieCandidates(MoviesConstant.NOW_PLAYING_API_PATH)
                TrailerSource.UPCOMING -> movieCandidates(MoviesConstant.UPCOMING_API_PATH)
                TrailerSource.POPULAR -> movieCandidates(MoviesConstant.POPULAR_API_PATH)
                TrailerSource.ON_TV -> tvCandidates(TvShowsConstant.ON_THE_AIR_API_PATH)
            }
        val limit = Semaphore(MAX_CONCURRENT_VIDEO_REQUESTS)
        // Children inherit this coroutine's Main dispatcher, so these appends never race.
        val found = mutableListOf<Trailer>()
        coroutineScope {
            candidates.take(MAX_TITLES_PER_SOURCE).forEach { candidate ->
                launch {
                    val trailer = limit.withPermit { fetchTrailerOrNull(candidate) } ?: return@launch
                    found += trailer
                    onProgress(found.orderedForRail())
                }
            }
        }
        return found.orderedForRail()
    }

    private suspend fun fetchTrailerOrNull(candidate: TrailerCandidate): Trailer? {
        val videos = fetchVideosOrNull(candidate) ?: return null
        return latestTrailerVideo(videos)?.let { video ->
            Trailer(
                mediaId = candidate.mediaId,
                isMovie = candidate.isMovie,
                mediaTitle = candidate.title,
                backdropPath = candidate.backdropPath,
                video = video,
            )
        }
    }

    // The trailer row is keyed by video id; a title TMDB lists twice on one page would otherwise
    // repeat its trailer and crash the row ("Key ... was already used"). Newest first.
    private fun List<Trailer>.orderedForRail(): List<Trailer> = distinctBy { it.video.id }.sortedByDescending { it.video.publishedAt }

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

        /** Videos calls in flight at once while building the trailer rail. */
        internal const val MAX_CONCURRENT_VIDEO_REQUESTS = 3
    }
}
