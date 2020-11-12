package com.clearkeep.chat.single.compose

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
import androidx.navigation.NavHostController
import com.clearkeep.chat.common_views.MessageListView
import com.clearkeep.chat.common_views.SendBottomCompose
import com.clearkeep.chat.single.PeerChatViewModel

@Composable
fun PeerChatScreen(
        navController: NavHostController,
        myClientId: String,
        roomName: String,
        roomId: Int,
        receiverId: String,
        singleChatViewModel: PeerChatViewModel
) {
    val messageList = singleChatViewModel.getMessageList(roomId).observeAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
                title = {
                    Text(text = roomName)
                },
        )
        Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
                0.66f
        )) {
            messageList?.let {
                MessageListView(
                        messageList = it.value ?: emptyList(),
                        myClientId = myClientId
                )
            }
        }
        SendBottomCompose(
                onSendMessage = { message -> singleChatViewModel.sendMessage(roomId, receiverId, message) }
        )
        Spacer(modifier = Modifier.height(60.dp))
    }
}