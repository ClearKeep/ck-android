package com.clearkeep.screen.chat.room.composes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.rounded.ArrowDownward
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

var mIsNewMessage = true

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageListView(
    messageList: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean,
    isNewMessage: Boolean = true
) {
    printlnCK("isNewMessage = $isNewMessage")
    mIsNewMessage = isNewMessage
    MessageListView(
        messageList = messageList,
        clients = clients,
        myClientId = myClientId,
        isGroup = isGroup,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageListView(
    messageList: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean,
) {
    val reversedMessage = messageList.reversed()
    Box() {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        if (listState.firstVisibleItemIndex == 0) {
            mIsNewMessage = false
        }

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
                        if (isMyMessage) OurMessage(item) else FriendMessage(item, userName, isGroup)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        val showButton = remember {
            derivedStateOf {
                val isBottom = listState.firstVisibleItemIndex == 0
                if (isBottom) {
                    mIsNewMessage = false
                }
                !isBottom
            }
        }
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (showButton.value) {
                ScrollToButtonButton(
                    isNewMessage = mIsNewMessage,
                    onClick = {
                        mIsNewMessage = false
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
    Column(modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isNewMessage) Text(text = "new message", style = MaterialTheme.typography.caption.copy(color = Color.Blue))
        Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = "", tint = Color.Blue)
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