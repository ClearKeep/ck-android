package com.clearkeep.screen.chat.main.chat

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.components.ckDividerColor

@Composable
fun ChatHistoryScreen(
        chatViewModel: ChatViewModel,
        onRoomSelected: (ChatGroup) -> Unit,
        onCreateGroup: () -> Unit
) {
    val rooms = chatViewModel.groups.observeAsState()
    Column {
        TopAppBar(
                title = {
                    Text(text = "Chat")
                },
                actions = {
                    IconButton(onClick = onCreateGroup) {
                        Icon(Icons.Filled.Add)
                    }
                }
        )
        rooms?.let {
            LazyColumnFor(
                    items = it?.value?.data ?: emptyList(),
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
            ) { room ->
                Surface(color = Color.White) {
                    RoomItem(
                            room,
                            chatViewModel.getClientName(),
                            onRoomSelected
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun RoomItem(
        room: ChatGroup,
        ourClientName: String,
        onRoomSelected: (ChatGroup) -> Unit,
) {
    val roomName = if (room.isGroup()) room.groupName else {
        val userNameList = room.groupName.split(",")
        userNameList.firstOrNull { userName ->
            userName != ourClientName
        } ?: ""
    }
    Column(modifier = Modifier
            .clickable(onClick = { onRoomSelected(room) }, enabled = true)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row() {
            Text(text = roomName,
                    style = MaterialTheme.typography.h6
            )
        }
        Text(text = room.lastMessage,
                style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = ckDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
    }
}