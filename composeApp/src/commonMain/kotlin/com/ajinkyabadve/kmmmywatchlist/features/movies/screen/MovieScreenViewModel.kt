package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Assuming you have an injectable repository
class MovieScreenViewModel(
    private val repository: MovieRepository = MovieRepositoryImpl()// Dependency Injection recommended
) : ViewModel() {

    // Internal mutable state
    private val _moviesState = MutableStateFlow(PagedList<Movie>())
    // Public immutable state consumed by the UI
    val moviesState: StateFlow<PagedList<Movie>> = _moviesState.asStateFlow()

    // Configuration for pagination
    private val pageSize = 20
    private var currentQuery = "popular" // Example: "popular", "top_rated", etc.

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        // If already loading or if the end has been reached, do nothing.
        val currentState = _moviesState.value
        if (currentState.isLoading || currentState.endReached) {
            return
        }

        // Set loading state
        _moviesState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val nextPage = currentState.currentPage + 1

                // --- Replace this mock call with your actual API call ---
                // Example: val result = repository.getMovies(currentQuery, nextPage)

                // Mock API call simulation:
                // If the repository returns a list and total pages:
                val newMovies = createMockMovies(pageSize) // Replace with API data
                val totalPages = 5 // Replace with API data

                _moviesState.update { oldState ->
                    val allItems = oldState.items + newMovies

                    PagedList(
                        items = allItems,
                        currentPage = nextPage,
                        totalPages = totalPages,
                        isLoading = false,
                        error = null,
                        endReached = nextPage >= totalPages
                    )
                }

            } catch (e: Exception) {
                _moviesState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load page ${it.currentPage}: ${e.message}",
                        endReached = false
                    )
                }
            }
        }
    }

    // Function to reset the list and start fresh (e.g., when search query changes)
    fun resetPagination(newQuery: String? = null) {
        if (newQuery != null) {
            currentQuery = newQuery
        }
        _moviesState.value = PagedList(isLoading = true) // Reset state to default
        loadNextPage()
    }

    // --- Mock Data function (REMOVE in production) ---
    private fun createMockMovies(count: Int): List<Movie> {
        return (1..count).map {
            Movie(id = it + (_moviesState.value.items.size), title = "Movie Title $it")
        }
    }
}

// Define this in a separate file, or near your ViewModel
data class PagedList<T>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)