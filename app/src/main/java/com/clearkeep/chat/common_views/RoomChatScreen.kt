package com.clearkeep.chat.common_views

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.clearkeep.db.model.Message

@Composable
fun RoomChatScreen(
        navController: NavHostController,
        myClientId: String,
        roomName: String,
        messageList: List<Message>,
        onSendMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
                title = {
                    Text(text = roomName)
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
        )
        Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
            0.66f
        )) {
            MessageListView(
                messageList = messageList,
                myClientId = myClientId
            )
        }
        SendBottomCompose(
            onSendMessage = onSendMessage
        )
    }
}