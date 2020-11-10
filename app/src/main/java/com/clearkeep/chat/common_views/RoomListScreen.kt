package com.clearkeep.chat.common_views

import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.loadVectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.db.model.Room
import androidx.navigation.compose.navigate
import androidx.ui.tooling.preview.Preview
import com.clearkeep.R
import com.clearkeep.ui.ckDividerColor

@Composable
fun RoomListScreen(
        navController: NavHostController,
        rooms: List<Room>,
        onRoomSelected: (Room) -> Unit,
        onFinishActivity: () -> Unit,
) {
    Column {
        TopAppBar(
                title = {
                    Text(text = "Group Chatting")
                },
                navigationIcon = {
                    IconButton(
                            onClick = {
                                /*val popped = navController.popBackStack(navController.graph.startDestination, false)
                                if (!popped) {
                                    finish()
                                }*/
                                onFinishActivity()
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("createGroup")
                    }) {
                        Icon(asset = Icons.Filled.Add)
                    }
                }
        )
        LazyColumnFor(
                items = rooms,
                contentPadding = PaddingValues(top = 20.dp, end = 20.dp),
        ) { room ->
            Surface(color = Color.White) {
                RoomItem(room, onRoomSelected)
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
        Text(text = "group id: ${room.remoteId}",
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
            Room("Room Name", "test", true),
            onRoomSelected = {}
    )
}