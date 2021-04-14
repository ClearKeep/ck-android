package com.clearkeep.screen.chat.room.composes

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearkeep.components.grayscaleOffWhite
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
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
    Surface(
        color = grayscaleOffWhite
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        if (listState.firstVisibleItemIndex == 0) {
            mIsNewMessage = false
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            state = listState,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        ) {
            items(reversedMessage) { item ->
                val isMyMessage = myClientId == item.senderId
                val userName = clients.firstOrNull {
                    it.id == item.senderId
                }?.userName ?: item.senderId
                Column {
                    if (isMyMessage) MessageByMe(item) else MessageFromOther(item, userName, isGroup)
                    Spacer(modifier = Modifier.height(16.dp))
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