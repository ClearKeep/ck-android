package com.clearkeep.chat.main.chat

import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearkeep.db.model.Room
import androidx.ui.tooling.preview.Preview
import com.clearkeep.ui.ckDividerColor

@Composable
fun ChatHistoryScreen(
        groupChatViewModel: ChatViewModel,
        onRoomSelected: (Room) -> Unit,
) {
    val rooms = groupChatViewModel.getChatHistoryList().observeAsState()
    Column {
        TopAppBar(
                title = {
                    Text(text = "Chat")
                },
        )
        rooms?.let {
            LazyColumnFor(
                    items = it?.value ?: emptyList(),
                    contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
            ) { room ->
                Surface(color = Color.White) {
                    RoomItem(room, onRoomSelected)
                }
            }
        }
    }
}

@Composable
fun RoomItem(
        room: Room,
        onRoomSelected: (Room) -> Unit,
) {
    Column(modifier = Modifier
            .clickable(onClick = { onRoomSelected(room) }, enabled = true)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row() {
            Text(text = room.roomName,
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

@Preview
@Composable
fun RoomItemPreview() {
    RoomItem(
            Room(0, "Room Name", "vandai", false,
                    true, "vandai", "hello", 0, false),
            onRoomSelected = {}
    )
}