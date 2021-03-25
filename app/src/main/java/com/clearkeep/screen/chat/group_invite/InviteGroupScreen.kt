package com.clearkeep.screen.chat.group_invite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.components.ckDividerColor
import com.clearkeep.components.colorTest
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.home.composes.CircleAvatar

@Composable
fun InviteGroupScreenComingSoon(
    inviteGroupViewModel: InviteGroupViewModel,

    onFriendSelected: (List<People>) -> Unit,
    onBackPressed: () -> Unit,
    isSelectionOnly: Boolean = false,
    selectedItem: SnapshotStateList<People>
) {
    Column {
        CKTopAppBar(
            title = {
                Text(text = "Add members")
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackPressed
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            },
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Coming soon", style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun InviteGroupScreen(
        inviteGroupViewModel: InviteGroupViewModel,

        onFriendSelected: (List<People>) -> Unit,
        onBackPressed: () -> Unit,
        isSelectionOnly: Boolean = false,
        selectedItem: SnapshotStateList<People>
) {
    val friends = inviteGroupViewModel.friends.observeAsState()

    Column {
        CKTopAppBar(
            title = {
                Text(text = if (isSelectionOnly) "Select friends" else "Invite Friends")
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackPressed
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            },
            actions = {
                CKTextButton(title = if (isSelectionOnly) "Next" else "Invite",
                    onClick = {
                        if (isSelectionOnly) {
                            onFriendSelected(selectedItem)
                        } else {
                            inviteGroupViewModel.inviteFriends(selectedItem)
                        }
                    },
                    enabled = !selectedItem.isEmpty()
                )
            }
        )
        friends?.value?.data?.let { values ->
            LazyColumn(
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
            ) {
                itemsIndexed(values) { _, friend ->
                    Surface(color = Color.White) {
                        FriendItem(friend,
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

@Composable
fun FriendItem(
        friend: People,
        isSelected: Boolean,
        onFriendSelected: (people: People, isAdd: Boolean) -> Unit,
) {
    Column(modifier = Modifier
        .selectable(
            selected = isSelected,
            onClick = { onFriendSelected(friend, !isSelected) })
        .padding(horizontal = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar(emptyList(), friend.userName)
            Column(modifier = Modifier
                .padding(start = 20.dp)
                .weight(1.0f, true)) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                )
            }
            RadioButton(
                    colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Blue,
                            unselectedColor = colorTest,
                            disabledColor = colorTest.copy(alpha = ContentAlpha.disabled)
                    ),
                    selected = isSelected,
                    onClick = { onFriendSelected(friend, !isSelected) }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = ckDividerColor, thickness = 0.3.dp, modifier = Modifier.padding(start = 68.dp))
        Spacer(modifier = Modifier.height(10.dp))
    }
}