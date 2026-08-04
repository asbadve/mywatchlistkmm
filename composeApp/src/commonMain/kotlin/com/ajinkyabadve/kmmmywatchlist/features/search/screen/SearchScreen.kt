package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchFilter
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.search_back_content_description
import mywatchlist.composeapp.generated.resources.search_clear_content_description
import mywatchlist.composeapp.generated.resources.search_field_hint
import mywatchlist.composeapp.generated.resources.search_no_results
import mywatchlist.composeapp.generated.resources.search_no_results_for_filter
import mywatchlist.composeapp.generated.resources.search_prompt
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchScreenModel = viewModel { SearchScreenModel() },
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onBackClicked: () -> Unit = {},
    onMovieSelected: (movieId: Long) -> Unit = {},
    onTvShowSelected: (tvShowId: Long) -> Unit = {},
    onPersonSelected: (personId: Long) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    // Held as TextFieldValue rather than a plain String so the caret can be placed explicitly.
    // Reopening search restores the previous query (the model outlives this composable), and a
    // String-valued TextField starts that restored text with the selection collapsed at index 0 -
    // so the first thing typed lands in front of the existing query ("matrix" + "love" came out as
    // "lovematrix"). Seeding the selection at the end puts the caret after the last character.
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = viewModel.query,
                selection = TextRange(viewModel.query.length),
            ),
        )
    }

    // The whole point of pushing a dedicated screen on search-box tap: the caret is already in the
    // field (and the soft keyboard already up) so the user types straight away.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val shouldPaginate =
        remember {
            derivedStateOf {
                val lastVisible =
                    lazyGridState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -1
                lastVisible >= lazyGridState.layoutInfo.totalItemsCount - PAGINATION_LOOKAHEAD_ITEMS
            }
        }
    LaunchedEffect(shouldPaginate.value, viewModel.listState) {
        if (shouldPaginate.value && viewModel.listState == ListState.IDLE) {
            viewModel.loadNextPage()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            fieldValue = fieldValue,
            focusRequester = focusRequester,
            onFieldValueChange = { newValue ->
                fieldValue = newValue
                viewModel.onQueryChange(newValue.text)
            },
            onClearClicked = {
                fieldValue = TextFieldValue("")
                viewModel.clearQuery()
            },
            onBackClicked = onBackClicked,
        )
        scrollableChips(
            selectedChip = SearchFilter.entries.indexOf(viewModel.selectedFilter),
            chipItemList = SearchFilter.entries.map { stringResource(it.label) },
            onClick = { index -> viewModel.onFilterSelected(SearchFilter.entries[index]) },
        )
        SearchResults(
            viewModel = viewModel,
            lazyGridState = lazyGridState,
            onMovieSelected = onMovieSelected,
            onTvShowSelected = onTvShowSelected,
            onPersonSelected = onPersonSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SearchField(
    fieldValue: TextFieldValue,
    focusRequester: FocusRequester,
    onFieldValueChange: (TextFieldValue) -> Unit,
    onClearClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.search_back_content_description),
            )
        }
        TextField(
            value = fieldValue,
            onValueChange = onFieldValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            placeholder = {
                // Without the single-line clamp this hint wraps to two lines on a narrow phone
                // (the back arrow and leading icon eat most of the width), which stretches the
                // field to double height before a single character is typed.
                Text(
                    text = stringResource(Res.string.search_field_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (fieldValue.text.isNotEmpty()) {
                    IconButton(onClick = onClearClicked) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(Res.string.search_clear_content_description),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
        )
    }
}

@Composable
private fun SearchResults(
    viewModel: SearchScreenModel,
    lazyGridState: LazyGridState,
    onMovieSelected: (movieId: Long) -> Unit,
    onTvShowSelected: (tvShowId: Long) -> Unit,
    onPersonSelected: (personId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = viewModel.results

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(minSize = GRID_MIN_CELL_SIZE),
        modifier = modifier,
    ) {
        items(results, key = { it.uniqueKey }) { item ->
            SearchResultCard(
                item = item,
                onClick = {
                    when (item.mediaType) {
                        SearchMediaType.MOVIE -> onMovieSelected(item.id.toLong())
                        SearchMediaType.TV -> onTvShowSelected(item.id.toLong())
                        SearchMediaType.PERSON -> onPersonSelected(item.id.toLong())
                        null -> Unit
                    }
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchStatus(viewModel = viewModel, hasResults = results.isNotEmpty())
        }
    }
}

@Composable
private fun SearchStatus(
    viewModel: SearchScreenModel,
    hasResults: Boolean,
) {
    when {
        viewModel.listState == ListState.LOADING -> CenteredBox { CircularProgressIndicator() }
        viewModel.listState == ListState.PAGINATING -> CenteredBox { CircularProgressIndicator() }
        viewModel.listState == ListState.ERROR || viewModel.listState == ListState.NETWORK_ERROR ->
            CenteredBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    viewModel.errorMessage?.let { Text(it.asString(), textAlign = TextAlign.Center) }
                    TextButton(onClick = viewModel::retry) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }
        // Nothing typed yet - prompt rather than an empty void.
        viewModel.submittedQuery.isBlank() ->
            CenteredBox {
                Text(stringResource(Res.string.search_prompt), textAlign = TextAlign.Center)
            }
        !hasResults ->
            CenteredBox {
                Text(
                    text =
                        if (viewModel.selectedFilter == SearchFilter.ALL) {
                            stringResource(Res.string.search_no_results, viewModel.submittedQuery)
                        } else {
                            // The query did match something, just not in the selected type - say so
                            // rather than implying the search itself came back empty.
                            stringResource(
                                Res.string.search_no_results_for_filter,
                                stringResource(viewModel.selectedFilter.label),
                                viewModel.submittedQuery,
                            )
                        },
                    textAlign = TextAlign.Center,
                )
            }
        else -> Unit
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(STATUS_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current.density
    val imageUrl =
        ImageConfigResolver.resolve(
            path = item.imagePath,
            type =
                if (item.mediaType == SearchMediaType.PERSON) {
                    ImageConfigResolver.ImageType.PROFILE
                } else {
                    ImageConfigResolver.ImageType.POSTER
                },
            targetWidthDp = GRID_MIN_CELL_SIZE.value.toInt(),
            density = density,
        )

    Column {
        Box {
            searchMediaRow(
                imageUrl = imageUrl,
                title = item.displayTitle,
                mediaType = item.mediaType,
                modifier = Modifier,
                onClick = onClick,
            )
            item.mediaType?.let { type ->
                MediaTypeBadge(
                    mediaType = type,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(BADGE_OFFSET),
                )
            }
        }
        item.releaseYear?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = YEAR_HORIZONTAL_PADDING),
            )
        }
    }
}

private val SCREEN_HORIZONTAL_PADDING = 8.dp
private val GRID_MIN_CELL_SIZE = 150.dp
private val STATUS_PADDING = 24.dp
private val BADGE_OFFSET = 12.dp
private val YEAR_HORIZONTAL_PADDING = 16.dp
private const val PAGINATION_LOOKAHEAD_ITEMS = 3
