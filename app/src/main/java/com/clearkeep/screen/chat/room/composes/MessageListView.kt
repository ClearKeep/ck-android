package com.clearkeep.screen.chat.room.composes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.utilities.getTimeAsString
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageListView(
    messageList: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean,
    /*isOnBottom: Boolean,
    onBottomButtonClick: () -> Unit*/
) {
    val reversedMessage = messageList.reversed()
    Box() {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var isNewMessage = remember {listState.firstVisibleItemIndex != 0}
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            state = listState,
            verticalArrangement = Arrangement.Top
        ) {
            items(reversedMessage) { item ->
                val isMyMessage = myClientId == item.senderId
                val userName = clients.firstOrNull {
                    it.id == item.senderId
                }?.userName ?: item.senderId
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isMyMessage) 60.dp else 0.dp,
                                end = if (isMyMessage) 0.dp else 60.dp,
                            ),
                        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
                    ) {
                        /*if (!item.message.isNullOrEmpty()) {
                            if (isMyMessage) OurMessage(item) else FriendMessage(item, userName, isGroup)
                        }*/
                        if (isMyMessage) OurMessage(item) else FriendMessage(item, userName, isGroup)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        val showButton = remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0
            }
        }
        Row(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(visible = showButton.value) {
                ScrollToButtonButton(
                    isNewMessage = isNewMessage,
                    onClick = {
                        isNewMessage = false
                        coroutineScope.launch {
                            listState.scrollToItem(0)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ScrollToButtonButton(isNewMessage: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        if (isNewMessage) Text(text = "new message")
        Icon(imageVector = Icons.Filled.ArrowCircleDown, contentDescription = "")
    }
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