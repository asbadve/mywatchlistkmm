package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.list_create_button
import mywatchlist.composeapp.generated.resources.list_create_title
import mywatchlist.composeapp.generated.resources.list_description_hint
import mywatchlist.composeapp.generated.resources.list_item_count
import mywatchlist.composeapp.generated.resources.list_name_hint
import mywatchlist.composeapp.generated.resources.lists_empty_message
import org.jetbrains.compose.resources.stringResource

private object ListsTabConstant {
    const val PAGINATION_LOOKAHEAD_ITEMS = 3
}

@Composable
fun ListsTab(
    session: UserSession,
    onListSelected: (listId: Long) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ListsScreenModel =
        viewModel(key = "ListsScreenModel:${session.accountId}") {
            ListsScreenModel(accountId = session.accountId, sessionId = session.sessionId)
        },
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    // Re-syncs on every mount, not just on a manual pull-to-refresh: `viewModel(key = ...)`
    // returns the same cached ScreenModel instance across a tab switch-away-and-back, so a list
    // created/deleted elsewhere would stay invisible here otherwise - see
    // AccountFavoritesWatchlistTab's kdoc for the full reasoning (same fix, same cause).
    LaunchedEffect(screenModel) {
        screenModel.refresh()
    }

    val lazyListState = rememberLazyListState()
    val shouldPaginate =
        remember {
            derivedStateOf {
                (
                    lazyListState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -ListsTabConstant.PAGINATION_LOOKAHEAD_ITEMS
                ) >=
                    (lazyListState.layoutInfo.totalItemsCount - ListsTabConstant.PAGINATION_LOOKAHEAD_ITEMS)
            }
        }
    LaunchedEffect(shouldPaginate.value, screenModel.listState) {
        if (shouldPaginate.value && screenModel.listState == ListState.IDLE) {
            screenModel.load()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = screenModel.listState == ListState.LOADING,
            onRefresh = { screenModel.refresh() },
        ) {
            LazyColumn(state = lazyListState) {
                item { NewListRow(onClick = { showCreateDialog = true }) }
                items(screenModel.lists, key = { it.id }) { list ->
                    ListRow(list = list, onClick = { onListSelected(list.id) })
                }
                item {
                    when (screenModel.listState) {
                        ListState.LOADING, ListState.PAGINATING ->
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                        ListState.PAGINATION_EXHAUST ->
                            if (screenModel.lists.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.lists_empty_message),
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                )
                            }

                        else -> {}
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateListDialog(
                createListState = screenModel.createListState,
                onCreate = { name, description ->
                    screenModel.createList(name, description) { listId ->
                        showCreateDialog = false
                        onListSelected(listId)
                    }
                },
                onDismiss = {
                    showCreateDialog = false
                    screenModel.resetCreateListState()
                },
            )
        }
    }
}

@Composable
private fun NewListRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(Res.string.list_create_title),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ListRow(
    list: TmdbList,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = list.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                text = stringResource(Res.string.list_item_count, list.itemCount),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CreateListDialog(
    createListState: CreateListState,
    onCreate: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isCreating = createListState is CreateListState.Creating

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.list_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.list_name_hint)) },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.list_description_hint)) },
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (createListState is CreateListState.Error) {
                    Text(
                        text = createListState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), description.trim()) },
                enabled = name.isNotBlank() && !isCreating,
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(Res.string.list_create_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
