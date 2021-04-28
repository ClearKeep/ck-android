package com.clearkeep.screen.chat.group_invite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.components.*
import com.clearkeep.components.base.*
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.composes.FriendListItem
import com.clearkeep.screen.chat.composes.FriendListItemSelectable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun InviteGroupScreen(
    inviteGroupViewModel: InviteGroupViewModel,
    selectedItem: SnapshotStateList<People>,
    onFriendSelected: (List<People>) -> Unit,
    onDirectFriendSelected: (People) -> Unit,
    onBackPressed: () -> Unit,
    isCreatePeerGroup: Boolean = true
) {
    val friends = inviteGroupViewModel.filterFriends.observeAsState()
    val textSearch = remember { mutableStateOf("") }

    Surface(
        color = grayscaleOffWhite
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.0f, true),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "",
                            tint = grayscale1
                        )
                    }
                }

                if (!isCreatePeerGroup) CKTextButton(
                    title = "Next",
                    onClick = {
                        onFriendSelected(selectedItem)
                    },
                    enabled = !selectedItem.isEmpty(),
                    fontSize = 16.sp,
                    textButtonType = TextButtonType.Blue
                )
            }
            Spacer(modifier = Modifier.height(25.dp))
            Text("New Message", style = MaterialTheme.typography.h5.copy(
                color = grayscaleBlack,
            ))
            Spacer(modifier = Modifier.height(24.dp))
            CKSearchBox(textSearch)
            if (selectedItem.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedHorizontalBox(selectedItem,
                    unSelectItem = { people ->
                        selectedItem.remove(people)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("User in this Channel", style = MaterialTheme.typography.body2.copy(
                color = grayscale2,
                fontSize = 16.sp
            ))
            Spacer(modifier = Modifier.height(16.dp))
            friends?.value?.let { values ->
                LazyColumn(
                    contentPadding = PaddingValues(end = 16.dp),
                ) {
                    itemsIndexed(values) { _, friend ->
                        if (isCreatePeerGroup) {
                            FriendListItem(friend, onFriendSelected = {
                                onDirectFriendSelected(it)
                            })
                        } else {
                            FriendListItemSelectable(friend,
                                selectedItem.contains(friend),
                                onFriendSelected = { people, isAdd ->
                                    if (isAdd) selectedItem.add(people) else selectedItem.remove(people)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(textSearch) {
        snapshotFlow { textSearch.value }
            .distinctUntilChanged()
            .collect {
                inviteGroupViewModel.search(it)
            }
    }
}

@Composable
fun SelectedHorizontalBox(selectedItem: List<People>, unSelectItem: (people: People) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(selectedItem) { _, friend ->
            SelectedFriendBox(people = friend, onRemove = { unSelectItem(it) })
        }
    }
}