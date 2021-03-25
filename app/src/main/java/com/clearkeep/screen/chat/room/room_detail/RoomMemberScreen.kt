package com.clearkeep.screen.chat.room.room_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.components.base.CKTopAppBar
import com.clearkeep.components.ckDividerColor
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.home.composes.CircleAvatar
import com.clearkeep.screen.chat.room.RoomViewModel

@Composable
fun GroupMemberScreen(
    roomViewModel: RoomViewModel,
    navHostController: NavHostController,
) {
    val groupState = roomViewModel.group.observeAsState()
    groupState?.value?.let { group ->
        Column {
            CKTopAppBar(
                title = {
                    Text(text = "Group members")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(30.dp))
            LazyColumn(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
            ) {
                itemsIndexed(group.clientList) { _, friend ->
                    Surface(color = Color.White) {
                        FriendItem(friend)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(
        friend: People,
) {
    Column() {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            CircleAvatar("")
            Column(modifier = Modifier.padding(start = 20.dp).fillMaxWidth()) {
                Text(text = friend.userName,
                        style = MaterialTheme.typography.h6
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = ckDividerColor, thickness = 0.3.dp, modifier = Modifier.padding(start = 68.dp))
        Spacer(modifier = Modifier.height(10.dp))
    }
}