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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.list_create_button
import mywatchlist.composeapp.generated.resources.list_create_title
import mywatchlist.composeapp.generated.resources.list_description_hint
import mywatchlist.composeapp.generated.resources.list_item_count
import mywatchlist.composeapp.generated.resources.list_name_hint
import mywatchlist.composeapp.generated.resources.lists_empty_message
import org.jetbrains.compose.resources.stringResource

/**
 * No longer needs an explicit refresh/mount effect or manual pagination trigger -
 * `QueryPagingSource` (see `CustomListRepository.pagedFlow`) auto-invalidates whenever anything
 * writes to the local `customList` table, and `collectAsLazyPagingItems()` drives its own
 * scroll-triggered page fetches - see `AccountFavoritesWatchlistTab`'s identical kdoc for the full
 * reasoning (same fix, same cause, now shared by both grids).
 */
@Composable
fun ListsTab(
    session: UserSession,
    onListSelected: (listId: Long) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ListsScreenModel =
        viewModel(key = "ListsScreenModel:${session.accountId}") {
            ListsScreenModel(accountId = session.accountId, sessionId = session.sessionId)
        },
    // Hoisted by MyFavTabs so re-tapping the already-selected tab can scroll this list to top.
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val lazyPagingItems = screenModel.pagedLists.collectAsLazyPagingItems()

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading,
            onRefresh = { lazyPagingItems.refresh() },
        ) {
            LazyColumn(state = lazyListState) {
                item { NewListRow(onClick = { showCreateDialog = true }) }
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id },
                ) { index ->
                    val list = lazyPagingItems[index] ?: return@items
                    ListRow(list = list, onClick = { onListSelected(list.id) })
                }
                item {
                    when (val appendState = lazyPagingItems.loadState.append) {
                        is LoadState.Loading ->
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                        is LoadState.Error ->
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(appendState.error.message.orEmpty(), textAlign = TextAlign.Center)
                                Button(onClick = { lazyPagingItems.retry() }) { Text(stringResource(Res.string.action_retry)) }
                            }

                        is LoadState.NotLoading ->
                            if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh is LoadState.NotLoading) {
                                Text(
                                    text = stringResource(Res.string.lists_empty_message),
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                )
                            }
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
