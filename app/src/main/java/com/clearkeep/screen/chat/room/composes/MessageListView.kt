package com.clearkeep.screen.chat.room.composes

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
import androidx.ui.tooling.preview.Preview
import com.clearkeep.db.model.Message

@Composable
fun MessageListView(
    messageList: List<Message>,
    myClientId: String
) {
    LazyColumnFor(
            items = messageList,
    ) { item ->
        val isMyMessage = myClientId == item.senderId
        Column {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(
                            start = if (isMyMessage) 0.dp else 60.dp,
                            end = if (isMyMessage) 60.dp else 0.dp,
                    ),
                    horizontalArrangement = if (isMyMessage) Arrangement.Start else Arrangement.End,
            ) {
                if (isMyMessage) OurMessage(item) else FriendMessage(item)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun OurMessage(message: Message) {
    Column {
        RoundCornerMessage(
                message = message.message,
                backgroundColor = MaterialTheme.colors.secondary,
                textColor = MaterialTheme.colors.onSecondary,
        )
    }
}

@Composable
fun FriendMessage(message: Message) {
    Column {
        Text(
                text = message.senderId,
                style = MaterialTheme.typography.caption
        )
        RoundCornerMessage(
                message = message.message,
                backgroundColor = MaterialTheme.colors.surface,
                textColor = MaterialTheme.colors.onSurface,
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

@Preview
@Composable
fun MessageListViewPreview() {
    MessageListView(
            myClientId = "dai",
            messageList = listOf(
                    Message("dai", "dai", "hello",  0, 1),
            )
    )
}