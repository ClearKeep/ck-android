package com.clearkeep.chat.single.views

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.db.model.Room
import androidx.navigation.compose.navigate

@Composable
fun RoomListScreen(
    navController: NavHostController,
    rooms: List<Room>
) {
    ScrollableColumn {
        rooms.forEach {
            Box(Modifier.clickable(
                onClick = {
                    navController.navigate("singleChatRoom/${it.remoteId}")
                }, enabled = true)) {
                Column {
                    Row {
                        Text(text = it.remoteId)
                    }
                }
                Divider(color = Color.Black, thickness = 2.dp)
            }
        }
    }
}