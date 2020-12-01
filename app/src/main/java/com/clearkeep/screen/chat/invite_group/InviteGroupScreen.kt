package com.clearkeep.screen.chat.invite_group

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearkeep.components.base.CKTextButton
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.components.ckDividerColor
import com.clearkeep.components.colorTest
import com.clearkeep.db.model.People
import com.clearkeep.screen.chat.main.composes.CircleAvatar

@Composable
fun InviteGroupScreen(
        inviteGroupViewModel: InviteGroupViewModel,

        onFriendSelected: (List<People>) -> Unit,
        onBackPressed: () -> Unit,
        isSelectionOnly: Boolean = false,
) {
    val friends = inviteGroupViewModel.friends.observeAsState()

    val selectedItem = remember { mutableStateListOf<People>() }

    Column {
        CKTopAppBar(
            title = {
                Text(text = if (isSelectionOnly) "Select friends" else "Invite Friends")
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackPressed
                ) {
                    Icon(asset = Icons.Filled.ArrowBack)
                }
            },
            actions = {
                /*Box(modifier = Modifier.clickable(onClick = {
                    if (isSelectionOnly) {
                        onFriendSelected(selectedItem)
                    } else {
                        inviteGroupViewModel.inviteFriends(selectedItem)
                    }
                })) {
                    Text(modifier = Modifier.padding(horizontal = 8.dp),
                        text = if (isSelectionOnly) "Next" else "Invite",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color.Blue
                        )
                    )
                }*/
                CKTextButton(title = if (isSelectionOnly) "Next" else "Invite",
                    onClick = {
                        if (isSelectionOnly) {
                            onFriendSelected(selectedItem)
                        } else {
                            inviteGroupViewModel.inviteFriends(selectedItem)
                        }
                    }
                )
            }
        )
        friends?.let {
            LazyColumnFor(
                    items = it?.value?.data ?: emptyList(),
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
            ) { friend ->
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

@Composable
fun FriendItem(
        friend: People,
        isSelected: Boolean,
        onFriendSelected: (people: People, isAdd: Boolean) -> Unit,
) {
    Column(modifier = Modifier.selectable(
            selected = isSelected,
            onClick = { onFriendSelected(friend, !isSelected) })
            .padding(horizontal = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar("")
            Column(modifier = Modifier.padding(start = 20.dp).weight(1.0f, true)) {
                Text(text = friend.userName,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                )
            }
            RadioButton(
                    colors = RadioButtonConstants.defaultColors(
                            selectedColor = Color.Blue,
                            unselectedColor = colorTest,
                            disabledColor = AmbientEmphasisLevels.current.disabled.applyEmphasis(
                                colorTest
                            )
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