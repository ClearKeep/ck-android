package com.clearkeep.chat.common_views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clearkeep.db.model.Message

@Composable
fun RoomChatScreen(
    myClientId: String,
    messageList: List<Message>,
    onSendMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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