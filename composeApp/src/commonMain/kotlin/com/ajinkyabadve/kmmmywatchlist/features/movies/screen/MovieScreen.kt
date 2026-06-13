import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import io.github.aakira.napier.Napier

@Composable
public fun MovieScreen(viewModel:MovieScreenViewModel =  MovieScreenViewModel()) {
    // Collect the state from the ViewModel
    val state by viewModel.moviesState.collectAsState()

    LazyColumn {
        items(state.items) { movie ->
            // Display your Movie item
//            MovieItem(movie)
            mediaMovieRow(
                MoviesTab.Tabs.IMAGE_BASE_URL + movie.posterPath,
                movie.title,
                modifier = Modifier,
                onClick = {
                    Napier.d { "title" + movie.title }
                },
            )
        }

        // Pagination logic at the end of the list
        item {
            if (state.isLoading) {
                // Show a loading indicator
                CircularProgressIndicator(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            } else if (state.error != null) {
                // Show error message and retry button
                Text("Error: ${state.error}")
                Button(onClick = { viewModel.loadNextPage() }) {
                    Text("Retry")
                }
            } else if (!state.endReached) {
                // Load more data when the user scrolls near the end
                // We typically trigger the load before the very last item is visible.
                DisposableEffect(Unit) {
                    viewModel.loadNextPage()
                    onDispose {}
                }
            } else {
                Text("You've reached the end of the list.")
            }
        }
    }
}