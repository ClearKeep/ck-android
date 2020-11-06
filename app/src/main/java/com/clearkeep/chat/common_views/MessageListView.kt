package com.clearkeep.chat.common_views

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clearkeep.db.model.Message

@Composable
fun MessageListView(
    messageList: List<Message>,
    myClientId: String
) {
    ScrollableColumn {
        messageList.forEach {
            val isMyMessage = myClientId == it.senderId
            Column (
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = if (isMyMessage) 0.dp else 60.dp,
                        end = if (isMyMessage) 60.dp else 0.dp,
                    )
                ) {
                    Text(
                        text = if (isMyMessage) it.message else "${it.senderId}: ${it.message}",
                        style = TextStyle(textAlign = if (isMyMessage) TextAlign.Start else TextAlign.End)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}