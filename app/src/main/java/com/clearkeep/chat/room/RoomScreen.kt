package com.clearkeep.chat.room

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.chat.room.composes.MessageListView
import com.clearkeep.chat.room.composes.SendBottomCompose

@Composable
fun RoomScreen(
    roomId: Int = 0,

    roomName: String,
    isGroup: Boolean,
    remoteId: String,
    isFromChatHistory: Boolean,

    roomViewModel: RoomViewModel,
    onFinishActivity: () -> Unit,
) {
    val messageList = if (isFromChatHistory) {
        roomViewModel.getMessagesInRoom(roomId).observeAsState()
    } else {
        roomViewModel.getMessagesWithFriend(remoteId).observeAsState()
    }
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
                                onFinishActivity()
                            }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
        )
        Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
            0.66f
        )) {
            messageList?.let {
                MessageListView(
                        messageList = it.value ?: emptyList(),
                        myClientId = roomViewModel.getClientId()
                )
            }
        }
        SendBottomCompose(
                onSendMessage = { message -> roomViewModel.sendMessage(isGroup, remoteId, message) }
        )
    }
}