package com.clearkeep.screen.chat.room.composes

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.getTimeAsString

@Composable
fun MessageListView(
    messageList: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean
) {
    ScrollableColumn(
        modifier = Modifier.fillMaxSize(),
        reverseScrollDirection = true,
    ) {
        messageList.forEach { item ->
            val isMyMessage = myClientId == item.senderId
            val userName = clients.firstOrNull {
                it.id == item.senderId
            }?.userName ?: item.senderId
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = if (isMyMessage) 60.dp else 0.dp,
                        end = if (isMyMessage) 0.dp else 60.dp,
                    ),
                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
                ) {
                    if (!item.message.isNullOrEmpty()) {
                        if (isMyMessage) OurMessage(item) else FriendMessage(item, userName, isGroup)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
    /*LazyColumnFor(
        items = messageList,
        contentPadding = PaddingValues(top = 30.dp, bottom = 30.dp),
    ) { item ->
        val isMyMessage = myClientId == item.senderId
        val userName = clients.firstOrNull {
            it.id == item.senderId
        }?.userName ?: item.senderId
        Column {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(
                            start = if (isMyMessage) 60.dp else 0.dp,
                            end = if (isMyMessage) 0.dp else 60.dp,
                    ),
                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
            ) {
                if (!item.message.isNullOrEmpty()) {
                    if (isMyMessage) OurMessage(item) else FriendMessage(item, userName, isGroup)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }*/
}

@Composable
fun OurMessage(message: Message) {
    Column {
        RoundCornerMessage(
                message = message.message,
                backgroundColor = Color(0xff5640fd),
                textColor = Color.White,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = getTimeAsString(message.createdTime),
            style = MaterialTheme.typography.caption.copy(fontSize = 8.sp)
        )
    }
}

@Composable
fun FriendMessage(message: Message, username: String, isGroup: Boolean) {
    Column {
        if (isGroup) Text(
                text = username,
                style = MaterialTheme.typography.caption
        )
        RoundCornerMessage(
                message = message.message,
                backgroundColor = Color(0xfff2f2f2),
                textColor = Color.Black,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = getTimeAsString(message.createdTime),
            style = MaterialTheme.typography.caption.copy(fontSize = 8.sp)
        )
    }
}

@Composable
fun RoundCornerMessage(message: String, backgroundColor: Color, textColor: Color) {
    Surface (
            color = backgroundColor,
            shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                    text = message,
                    style = MaterialTheme.typography.body2.copy(
                            color = textColor
                    )
            )
        }
    }
}