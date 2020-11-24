package com.clearkeep.screen.chat.invite_group

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.components.ckDividerColor
import com.clearkeep.db.model.People

@Composable
fun InviteGroupScreen(
        navController: NavHostController,
        inviteGroupViewModel: InviteGroupViewModel,

        onFriendSelected: (List<People>) -> Unit
) {
    val friends = inviteGroupViewModel.friends.observeAsState()

    val selectedItem: ArrayList<People> = ArrayList()

    Column {
        TopAppBar(
                title = {
                    Text(text = "Create group")
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                navController.popBackStack(navController.graph.startDestination, false)
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
                actions = {
                    Box(modifier = Modifier.clickable(onClick = {
                        onFriendSelected(selectedItem)
                    })) {
                        Text(text = "Continue")
                    }
                }
        )
        friends?.let {
            LazyColumnFor(
                items = it?.value?.data ?: emptyList(),
                contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
            ) { friend ->
                Surface(color = Color.White) {
                    FriendItem(friend,
                        onFriendSelected = {people ->
                            selectedItem.add(people)
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
    onFriendSelected: (People) -> Unit,
) {
    Column(modifier = Modifier
        .clickable(onClick = { onFriendSelected(friend) }, enabled = true)
        .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row() {
            Text(text = friend.userName,
                style = MaterialTheme.typography.h6
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = ckDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
    }
}