package com.clearkeep.screen.chat.room.composes

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.components.grayscale3
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.screen.chat.room.message_display_generator.convertMessageList
import com.clearkeep.utilities.getTimeAsString
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.sdp
import kotlinx.coroutines.launch
import kotlin.math.max

var mIsNewMessage = true

@ExperimentalFoundationApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageListView(
    messageList: List<Message>,
    clients: List<User>,
    listAvatar: List<User>,
    listState: LazyListState = rememberLazyListState(),
    myClientId: String,
    isGroup: Boolean,
    isNewMessage: Boolean = true,
    isLoading: Boolean = false,
    onScrollChange: (itemIndex: Int, lastMessageTimestamp: Long) -> Unit,
    onClickFile: (url: String) -> Unit,
    onClickImage: (uris: List<String>, senderName: String) -> Unit,
    onLongClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit,
    onQuoteClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit
) {
    mIsNewMessage = isNewMessage
    MessageListView(
        messageList = messageList,
        clients = clients,
        listAvatar = listAvatar,
        myClientId = myClientId,
        isGroup = isGroup,
        isLoading = isLoading,
        listState = listState,
        onScrollChange = onScrollChange,
        onClickFile = onClickFile,
        onClickImage = onClickImage,
        onLongClick = onLongClick,
        onQuoteClick = onQuoteClick
    )
}

@ExperimentalFoundationApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageListView(
    messageList: List<Message>,
    clients: List<User>,
    listAvatar: List<User>,
    listState: LazyListState = rememberLazyListState(),
    myClientId: String,
    isGroup: Boolean,
    isLoading: Boolean,
    onScrollChange: (itemIndex: Int, lastMessageTimestamp: Long) -> Unit,
    onClickFile: (url: String) -> Unit,
    onClickImage: (uris: List<String>, senderName: String) -> Unit,
    onLongClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit,
    onQuoteClick: (messageDisplayInfo: MessageDisplayInfo) -> Unit
) {
    val groupedMessages: Map<String, List<MessageDisplayInfo>> =
        messageList.filter { it.message.isNotBlank() || it.message != "" }
            .groupBy { getTimeAsString(it.createdTime) }.mapValues { entry ->
                convertMessageList(entry.value, clients, listAvatar, myClientId, isGroup)
            }
    Surface(
        color = MaterialTheme.colors.background
    ) {
        val coroutineScope = rememberCoroutineScope()
        val lastNewestItem = remember { mutableStateOf<Message?>(null) }

        val oldestVisibleItemIndex = listState.visibleItems(50f).map { it.index }.maxOrNull()
        val listMessageCanShow=messageList.filter { it.message.isNotBlank() || it.message.isNotEmpty() }
        LaunchedEffect(key1 = oldestVisibleItemIndex) {
            printlnCK("MessageListView oldestVisibleItemIndex $oldestVisibleItemIndex list size ${listMessageCanShow.size}")
            if (oldestVisibleItemIndex != null && oldestVisibleItemIndex >= listMessageCanShow.size - 1) {
                val oldestItem = listMessageCanShow[listMessageCanShow.size - 1]
                printlnCK("MessageListView oldest item $oldestItem")
                onScrollChange(oldestVisibleItemIndex, oldestItem.createdTime)
            }
        }
        if (listState.firstVisibleItemIndex == 0) {
            mIsNewMessage = false
        }

        if (messageList.isNotEmpty() && lastNewestItem.value != messageList[0] && messageList[0].senderId == myClientId) {
            mIsNewMessage = false
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
            lastNewestItem.value = messageList[0]
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            state = listState,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(start = 16.sdp(), end = 16.sdp()),
        ) {
            groupedMessages.forEach { (date, messages) ->
                itemsIndexed(messages,
                    key = { _: Int, item: MessageDisplayInfo ->
                        item.message.generateId ?: item.message.createdTime
                    }
                ) { index, item ->
                    Column {
                        if (index == messages.size - 1) {
                            DateHeader(date)
                        }
                        if (item.isOwner) MessageByMe(
                            item,
                            onClickFile,
                            onClickImage,
                            onLongClick,
                            onQuoteClick
                        ) else MessageFromOther(
                            item,
                            onClickFile,
                            onClickImage,
                            onLongClick,
                            onQuoteClick
                        )
                        if (index == 0) Spacer(modifier = Modifier.height(20.sdp()))
                    }
                }
                item {
                    if (isLoading) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.sdp())
                                .size(16.sdp())
                        ) {
                            CKCircularProgressIndicator(
                                Modifier
                                    .size(28.sdp())
                                    .align(Alignment.CenterHorizontally),
                                strokeWidth = 4.sdp()
                            )
                        }
                    }
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.sdp()),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (showButton.value) {
                ScrollToButtonButton(
                    isNewMessage = mIsNewMessage && messageList.isNotEmpty() && lastNewestItem.value != messageList[0] && messageList[0].senderId != myClientId,
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
fun DateHeader(date: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.sdp())
    ) {
        Text(
            date, style = MaterialTheme.typography.body2.copy(
                color = grayscale3,
                fontWeight = FontWeight.W600
            )
        )
    }
}

@Composable
fun ScrollToButtonButton(isNewMessage: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isNewMessage) Text(
            text = "new message",
            style = MaterialTheme.typography.caption.copy(color = Color.Blue)
        )
        Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = "", tint = Color.Blue)
    }
}

fun LazyListState.visibleItems(itemVisiblePercentThreshold: Float) =
    layoutInfo
        .visibleItemsInfo
        .filter {
            visibilityPercent(it) >= itemVisiblePercentThreshold
        }

fun LazyListState.visibilityPercent(info: LazyListItemInfo): Float {
    val cutTop = max(0f, (layoutInfo.viewportStartOffset - info.offset).toFloat())
    val cutBottom = max(0f, (info.offset + info.size - layoutInfo.viewportEndOffset).toFloat())

    return max(0f, (100f - (cutTop + cutBottom) * 100f / info.size))
}