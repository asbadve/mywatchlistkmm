package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.core.ui.MediaListRow
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.action_delete
import mywatchlist.composeapp.generated.resources.action_remove_from_list
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.list_delete_button
import mywatchlist.composeapp.generated.resources.list_delete_confirm_message
import mywatchlist.composeapp.generated.resources.list_delete_confirm_title
import mywatchlist.composeapp.generated.resources.lists_empty_message
import org.jetbrains.compose.resources.stringResource

private object ListDetailScreenConstant {
    const val YEAR_LENGTH = 4
    const val RATING_SCALE = 10
    const val RATING_DECIMAL = 10.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: Long,
    sessionId: String,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    viewModel: ListDetailScreenModel =
        viewModel(key = "ListDetailScreenModel:$listId") { ListDetailScreenModel(listId, sessionId) },
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.isDeleted) {
        if (viewModel.isDeleted) onBackClicked()
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = (viewModel.uiState as? ListDetailState.Success)?.detail?.name.orEmpty(),
                onBackClicked = onBackClicked,
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(Res.string.list_delete_button),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = viewModel.uiState) {
                is ListDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ListDetailState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.message.asString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        Button(onClick = { viewModel.loadListDetails() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is ListDetailState.Success -> {
                    if (state.detail.items.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.lists_empty_message),
                            modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.Center),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.detail.items, key = { it.id }) { movie ->
                                val year = movie.releaseDate.take(ListDetailScreenConstant.YEAR_LENGTH)
                                val rating =
                                    movie.voteAverage.takeIf { it > 0 }?.let {
                                        "${(it * ListDetailScreenConstant.RATING_SCALE).toInt() / ListDetailScreenConstant.RATING_DECIMAL} ★"
                                    }
                                val yearAndRating = listOfNotNull(year.takeIf { it.isNotEmpty() }, rating).joinToString(" • ")
                                MediaListRow(
                                    title = movie.title,
                                    posterPath = movie.posterPath,
                                    yearAndRating = yearAndRating,
                                    overview = movie.overview,
                                    onClick = { onMovieClicked(movie.id.toLong()) },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.removeMovie(movie.id.toLong()) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResource(Res.string.action_remove_from_list),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.list_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.list_delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteList()
                    },
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}
