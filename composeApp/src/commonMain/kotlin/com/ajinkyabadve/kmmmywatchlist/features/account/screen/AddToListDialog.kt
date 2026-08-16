package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import kotlinx.coroutines.launch
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_add_to_list
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.list_create_button
import mywatchlist.composeapp.generated.resources.list_create_title
import mywatchlist.composeapp.generated.resources.list_name_hint
import mywatchlist.composeapp.generated.resources.lists_empty_message
import org.jetbrains.compose.resources.stringResource

private object AddToListDialogConstant {
    val MAX_HEIGHT = 320.dp
}

/**
 * A movie's "add to list" picker - shows every list the user has, with a per-row "Add"/"Added"
 * state (this dialog does not know which lists already contain the movie, so a row always starts
 * as "Add" even if it's already a member; tapping it again is harmless - TMDB's add_item is
 * idempotent), plus an inline "new list" row so a list can be created and populated in one step.
 * Reuses [ListsScreenModel] for the list index, the same one `ListsTab` uses.
 */
@Composable
fun AddToListDialog(
    session: UserSession,
    movieId: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listsRepository: ListsRepository = ListsRepositoryImpl(),
    listsScreenModel: ListsScreenModel =
        viewModel(key = "ListsScreenModel:${session.accountId}") {
            ListsScreenModel(accountId = session.accountId, sessionId = session.sessionId)
        },
) {
    var addedListIds by remember { mutableStateOf(emptySet<Long>()) }
    var showCreateField by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(Res.string.action_add_to_list)) },
        text = {
            Column {
                if (showCreateField) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text(stringResource(Res.string.list_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = newListName.isNotBlank(),
                            onClick = {
                                val name = newListName.trim()
                                listsScreenModel.createList(name, "") { listId ->
                                    coroutineScope.launch {
                                        listsRepository.addMovieToList(listId, session.sessionId, movieId)
                                        addedListIds = addedListIds + listId
                                    }
                                }
                                showCreateField = false
                                newListName = ""
                            },
                        ) {
                            Text(stringResource(Res.string.list_create_button))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showCreateField = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Res.string.list_create_title), color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (listsScreenModel.lists.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.lists_empty_message),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = AddToListDialogConstant.MAX_HEIGHT)) {
                        items(listsScreenModel.lists, key = { it.id }) { list ->
                            AddToListRow(
                                list = list,
                                isAdded = list.id in addedListIds,
                                onAddClick = {
                                    coroutineScope.launch {
                                        listsRepository.addMovieToList(list.id, session.sessionId, movieId)
                                        addedListIds = addedListIds + list.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun AddToListRow(
    list: TmdbList,
    isAdded: Boolean,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = list.name, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
        if (isAdded) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = onAddClick) {
                Text(stringResource(Res.string.action_add_to_list))
            }
        }
    }
}
