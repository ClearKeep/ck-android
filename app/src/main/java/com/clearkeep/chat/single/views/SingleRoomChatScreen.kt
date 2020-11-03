package com.clearkeep.chat.single.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.chat.single.SingleChatViewModel
import com.clearkeep.db.model.Message

private var messageListState = mutableStateOf(emptyList<Message>())

@Composable
fun SingleRoomChatScreen(
    singleChatViewModel: SingleChatViewModel,
    receiverId: String
) {
    val onSendMessage: (String) -> Unit = {
        singleChatViewModel.sendMessage(receiverId, it)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp) + Modifier.weight(
            0.66f
        )) {
            MessageListView(
                state = messageListState
            )
        }
        SendBottomCompose(
            onSendMessage = onSendMessage
        )
    }
}